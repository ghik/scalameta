package scala.meta.internal.metacp

import java.io._
import java.nio.file._
import scala.collection.mutable
import scala.meta.internal.{semanticdb3 => s}
import scala.meta.internal.semanticdb3.Accessibility.{Tag => a}
import scala.meta.internal.semanticdb3.SymbolInformation.{Kind => k}
import scala.meta.internal.semanticdb3.SymbolInformation.{Property => p}
import scala.meta.internal.semanticdb3.SingletonType.{Tag => st}
import scala.meta.internal.semanticdb3.Type.{Tag => t}
import scala.reflect.NameTransformer
import scala.tools.scalap.scalax.rules.ScalaSigParserError
import scala.tools.scalap.scalax.rules.scalasig._
import scala.util.control.NonFatal
import org.langmeta.internal.io._
import org.langmeta.io._

object Main {
  def process(args: Array[String]): Int = {
    Settings.parse(args.toList) match {
      case Some(settings) => sys.exit(process(settings))
      case None => sys.exit(1)
    }
  }

  def process(settings: Settings): Int = {
    val semanticdbRoot = AbsolutePath(settings.d).resolve("META-INF").resolve("semanticdb")
    var failed = false
    val classpath = Classpath(settings.cps.mkString(File.pathSeparator))
    val fragments = classpath.deep.filter(_.uri.toString.endsWith(".class"))
    fragments.sortBy(_.uri.toString).foreach { fragment =>
      try {
        val bytecode = {
          val is = fragment.uri.toURL.openStream()
          try ByteCode(InputStreamIO.readBytes(is))
          finally is.close()
        }
        val classfile = ClassFileParser.parse(bytecode)
        ScalaSigParser.parse(classfile) match {
          case Some(scalaSig) =>
            val semanticdbInfos = scalaSig.symbols.flatMap {
              case sym: SymbolInfoSymbol => sinfo(sym)
              case _ => None
            }
            val className = NameTransformer.decode(PathIO.toUnix(fragment.name.toString))
            val semanticdbRelpath = fragment.name.resolveSibling(_ + ".semanticdb")
            val semanticdbAbspath = semanticdbRoot.resolve(semanticdbRelpath)
            val semanticdbDocument = s.TextDocument(
              schema = s.Schema.SEMANTICDB3,
              uri = className,
              language = Some(s.Language("Scala")),
              symbols = semanticdbInfos)
            val semanticdbDocuments = s.TextDocuments(List(semanticdbDocument))
            FileIO.write(semanticdbAbspath, semanticdbDocuments)
          case None =>
            // NOTE: If a classfile doesn't contain ScalaSignature,
            // we skip it for the time being. In the future, we may add support
            // for parsing arbitrary Java classfiles.
            ()
        }
      } catch {
        case NonFatal(ex) =>
          println(s"error: can't convert $fragment")
          ex.printStackTrace()
          failed = true
      }
    }
    if (failed) 1 else 0
  }

  private def sinfo(sym: SymbolInfoSymbol): Option[s.SymbolInformation] = {
    if (sym.parent.get == NoSymbol) return None
    if (sym.isSynthetic) return None // TODO: Implement me.
    if (sym.isModuleClass) return None
    if (sym.name.endsWith(" ")) return None // TODO: Implement me.
    if (sym.name.endsWith("_$eq")) return None // TODO: Implement me.
    if (sym.name == "<init>" && !sym.isClassConstructor) return None
    Some(s.SymbolInformation(
      symbol = ssymbol(sym),
      kind = skind(sym),
      properties = sproperties(sym),
      name = sname(sym),
      tpe = stpe(sym),
      annotations = sanns(sym),
      accessibility = Some(sacc(sym)),
      owner = sowner(sym)))
  }

  private def ssymbol(sym: Symbol): String = {
    val prefix = {
      sym match {
        case sym: SymbolInfoSymbol => ssymbol(sym.parent.get)
        case sym: ExternalSymbol => sym.parent.map(_.path + ".").getOrElse("")
        case _ => sys.error(s"unsupported symbol $sym")
      }
    }
    val encodedName = {
      val name = sname(sym)
      if (name.isEmpty) sys.error(s"unsupported symbol $sym")
      else {
        val (start, parts) = (name.head, name.tail)
        val isStartOk = Character.isJavaIdentifierStart(start)
        val isPartsOk = parts.forall(Character.isJavaIdentifierPart)
        if (isStartOk && isPartsOk) name
        else "`" + name + "`"
      }
    }
    skind(sym) match {
      case k.VAL | k.VAR | k.OBJECT | k.PACKAGE | k.PACKAGE_OBJECT =>
        prefix + encodedName + "."
      case k.DEF | k.GETTER | k.SETTER | k.PRIMARY_CONSTRUCTOR |
           k.SECONDARY_CONSTRUCTOR | k.MACRO =>
        val descriptor = {
          // TODO: Implement me.
          def loop(tpe: Type): String = {
            tpe match {
              case PolyType(tpe, _) => loop(tpe)
              case MethodType(_, params) => "(" + params.length + ")"
              case _ => "(0)"
            }
          }
          sym match {
            case sym: SymbolInfoSymbol => loop(sym.infoType)
            case sym => sys.error(s"unsupported symbol $sym")
          }
        }
        prefix + encodedName + descriptor + "."
      case k.TYPE | k.CLASS | k.TRAIT =>
        prefix + encodedName + "#"
      case k.PARAMETER =>
        prefix + "(" + encodedName + ")"
      case k.TYPE_PARAMETER =>
        prefix + "[" + encodedName + "]"
      case skind =>
        sys.error(s"unsupported kind $skind for symbol $sym")
    }
  }

  private val primaryCtors = mutable.Map[String, Int]()
  private def skind(sym: Symbol): s.SymbolInformation.Kind = {
    sym match {
      case sym: MethodSymbol if sym.isAccessor && !sym.isParamAccessor =>
        // TODO: Implement me.
        if (sym.isMutable) k.VAR
        else k.VAL
      case sym: MethodSymbol if sym.isParamAccessor || sym.isParam =>
        // NOTE: This is some craziness - parameters are modelled as methods.
        // Not just class parameters, but also normal method parameters.
        k.PARAMETER
      case sym: MethodSymbol if sym.name == "<init>" =>
        val primaryIndex = primaryCtors.getOrElseUpdate(sym.path, sym.entry.index)
        if (sym.entry.index == primaryIndex) k.PRIMARY_CONSTRUCTOR
        else k.SECONDARY_CONSTRUCTOR
      case sym: MethodSymbol =>
        // NOTE: More craziness - 0x8000 used to mean DEPRECATED back then.
        // Since then, deprecated became an annotation, and 0x8000 got used by MACRO,
        // but Scalap hasn't been updated.
        if (sym.isDeprecated) k.MACRO
        else k.DEF
      case sym: TypeSymbol if sym.isParam =>
        k.TYPE_PARAMETER
      case _: TypeSymbol | _: AliasSymbol =>
        k.TYPE
      case sym: ClassSymbol if !sym.isModule =>
        if (sym.isTrait) k.TRAIT
        else k.CLASS
      case _: ObjectSymbol | _: ClassSymbol if sym.isModule =>
        if (sym.name == "package") k.PACKAGE_OBJECT
        else k.OBJECT
      case sym: ExternalSymbol =>
        // NOTE: Object and package external symbols
        // are indistinguishable from each other.
        val hasTermName = {
          val idx = sym.entry.index + 1
          if (sym.entry.scalaSig.hasEntry(idx)) {
            val nameEntryType = sym.entry.scalaSig.table(idx)._1
            nameEntryType == 1
          } else {
            false
          }
        }
        val isModuleClass = sym.entry.entryType == 10
        if (hasTermName || isModuleClass) k.OBJECT
        else k.CLASS
      case NoSymbol =>
        k.UNKNOWN_KIND
      case _ =>
        sys.error(s"unsupported symbol $sym")
    }
  }

  private def sproperties(sym: SymbolInfoSymbol): Int = {
    def isAbstractClass = sym.isAbstract && !sym.isTrait
    def isAbstractMethod = sym.isMethod && sym.isDeferred
    def isAbstractType = sym.isType && !sym.isParam && sym.isDeferred
    var sproperties = 0
    def sflip(sbit: Int) = sproperties ^= sbit
    if (isAbstractClass || isAbstractMethod || isAbstractType) sflip(p.ABSTRACT.value)
    if (sym.isFinal || sym.isModule) sflip(p.FINAL.value)
    if (sym.isSealed) sflip(p.SEALED.value)
    if (sym.isImplicit) sflip(p.IMPLICIT.value)
    if (sym.isLazy) sflip(p.LAZY.value)
    if (sym.isCase) sflip(p.CASE.value)
    if (sym.isType && sym.isCovariant) sflip(p.COVARIANT.value)
    if (sym.isType && sym.isContravariant) sflip(p.CONTRAVARIANT.value)
    if (sym.isAccessor && sym.isParamAccessor && !sym.isMutable) sflip(p.VALPARAM.value)
    if (sym.isAccessor && sym.isParamAccessor && sym.isMutable) sflip(p.VARPARAM.value)
    sproperties
  }

  private def sname(sym: Symbol): String = {
    if (sym.name == "<no symbol>") ""
    else if (sym.name == "<root>") "_root_"
    else if (sym.name == "<empty>") "_empty_"
    else if (sym.name == "<init>") "<init>"
    else NameTransformer.decode(sym.name)
  }

  private def stpe(sym: SymbolInfoSymbol): Option[s.Type] = {
    def loop(tpe: Type): Option[s.Type] = {
      tpe match {
        case ByNameType(tpe) =>
          val stag = t.BY_NAME_TYPE
          val stpe = loop(tpe)
          Some(s.Type(tag = stag, byNameType = Some(s.ByNameType(stpe))))
        case RepeatedType(tpe) =>
          val stag = t.REPEATED_TYPE
          val stpe = loop(tpe)
          Some(s.Type(tag = stag, repeatedType = Some(s.RepeatedType(stpe))))
        case TypeRefType(pre, sym, args) =>
          val stag = t.TYPE_REF
          val spre = if (tpe.hasNontrivialPrefix) loop(pre) else None
          val ssym = ssymbol(sym)
          val sargs = args.flatMap(loop)
          Some(s.Type(tag = stag, typeRef = Some(s.TypeRef(spre, ssym, sargs))))
        case SingleType(pre, sym) =>
          val stag = t.SINGLETON_TYPE
          val stpe = {
            val stag = st.SYMBOL
            val spre = if (tpe.hasNontrivialPrefix) loop(pre) else None
            val ssym = ssymbol(sym)
            s.SingletonType(stag, spre, ssym, 0, "")
          }
          Some(s.Type(tag = stag, singletonType = Some(stpe)))
        case ThisType(sym) =>
          val stag = t.SINGLETON_TYPE
          val stpe = {
            val stag = st.THIS
            // TODO: Implement me.
            val spre = loop(TypeRefType(NoPrefixType, sym, Nil))
            s.SingletonType(stag, spre, "", 0, "")
          }
          Some(s.Type(tag = stag, singletonType = Some(stpe)))
        case ConstantType(underlying: Type) =>
          loop(underlying).map { sarg =>
            val stag = t.TYPE_REF
            val ssym = "_root_.java.lang.Class#"
            val sargs = sarg :: Nil
            // TODO: Implement me.
            s.Type(tag = stag, typeRef = Some(s.TypeRef(None, ssym, sargs)))
          }
        case ConstantType(const) =>
          val stag = t.SINGLETON_TYPE
          val stpe = {
            def floatBits(x: Float) = java.lang.Float.floatToRawIntBits(x).toLong
            def doubleBits(x: Double) = java.lang.Double.doubleToRawLongBits(x)
            const match {
              case () => s.SingletonType(st.UNIT, None, "", 0, "")
              case false => s.SingletonType(st.BOOLEAN, None, "", 0, "")
              case true => s.SingletonType(st.BOOLEAN, None, "", 1, "")
              case x: Byte => s.SingletonType(st.BYTE, None, "", x.toLong, "")
              case x: Short => s.SingletonType(st.SHORT, None, "", x.toLong, "")
              case x: Char => s.SingletonType(st.CHAR, None, "", x.toLong, "")
              case x: Int => s.SingletonType(st.INT, None, "", x.toLong, "")
              case x: Long => s.SingletonType(st.LONG, None, "", x, "")
              case x: Float => s.SingletonType(st.FLOAT,None, "", floatBits(x), "")
              case x: Double => s.SingletonType(st.DOUBLE, None, "", doubleBits(x), "")
              case x: String => s.SingletonType(st.STRING, None, "", 0, x)
              case null => s.SingletonType(st.NULL, None, "", 0, "")
              case other => sys.error(s"unsupported const $other")
            }
          }
          Some(s.Type(tag = stag, singletonType = Some(stpe)))
        case RefinedType(sym, parents) =>
          val stag = t.STRUCTURAL_TYPE
          val sparents = parents.flatMap(loop)
          val sdecls = sym.children.map(ssymbol)
          Some(s.Type(tag = stag, structuralType = Some(s.StructuralType(Nil, sparents, sdecls))))
        case AnnotatedType(tpe, anns) =>
          val stag = t.ANNOTATED_TYPE
          val sanns = {
            // TODO: Not supported by scalap.
            // anns.reverse.flatMap(sann)
            Nil
          }
          val stpe = loop(tpe)
          Some(s.Type(tag = stag, annotatedType = Some(s.AnnotatedType(sanns, stpe))))
        case ExistentialType(tpe, tparams) =>
          val stag = t.EXISTENTIAL_TYPE
          val stparams = tparams.map(ssymbol)
          val stpe = loop(tpe)
          Some(s.Type(tag = stag, existentialType = Some(s.ExistentialType(stparams, stpe))))
        case ClassInfoType(sym, parents) =>
          val stag = t.CLASS_INFO_TYPE
          val sparents = parents.flatMap(loop)
          val sdecls = sym.children.map(ssymbol)
          Some(s.Type(tag = stag, classInfoType = Some(s.ClassInfoType(Nil, sparents, sdecls))))
        case NullaryMethodType(tpe) =>
          val stag = t.METHOD_TYPE
          val stpe = loop(tpe)
          Some(s.Type(tag = stag, methodType = Some(s.MethodType(Nil, Nil, stpe))))
        case tpe: MethodType =>
          def flatten(tpe: Type): (List[List[Symbol]], Type) = {
            tpe match {
              case MethodType(tpe, head) =>
                val (tail, ret) = flatten(tpe)
                (head.toList +: tail, ret)
              case other =>
                (Nil, other)
            }
          }
          val (paramss, ret) = flatten(tpe)
          val stag = t.METHOD_TYPE
          val sparamss = paramss.map { params =>
            val sparams = params.map(ssymbol)
            s.MethodType.ParameterList(sparams)
          }
          val sret = loop(ret)
          Some(s.Type(tag = stag, methodType = Some(s.MethodType(Nil, sparamss, sret))))
        case TypeBoundsType(lo, hi) =>
          val stag = t.TYPE_TYPE
          val slo = loop(lo)
          val shi = loop(hi)
          Some(s.Type(tag = stag, typeType = Some(s.TypeType(Nil, slo, shi))))
        case PolyType(tpe, tparams) =>
          val stparams = tparams.map(ssymbol)
          loop(tpe).map { stpe =>
            if (stpe.tag == t.STRUCTURAL_TYPE) {
              stpe.update(_.structuralType.typeParameters := stparams)
            } else if (stpe.tag == t.CLASS_INFO_TYPE) {
              stpe.update(_.classInfoType.typeParameters := stparams)
            } else if (stpe.tag == t.METHOD_TYPE) {
              stpe.update(_.methodType.typeParameters := stparams)
            } else if (stpe.tag == t.TYPE_TYPE) {
              stpe.update(_.typeType.typeParameters := stparams)
            } else {
              val stag = t.UNIVERSAL_TYPE
              s.Type(tag = stag, universalType = Some(s.UniversalType(stparams, Some(stpe))))
            }
          }
        case NoType =>
          None
        case NoPrefixType =>
          None
        case other =>
          sys.error(s"unsupported type $other")
      }
    }

    try loop(sym.infoType)
    catch {
      case ScalaSigParserError("Unexpected failure") =>
        // See https://github.com/scalameta/scalameta/issues/1283
        // when this can happen
        None
    }
  }

  def sanns(sym: SymbolInfoSymbol): List[s.Annotation] = {
    // TODO: Implement me.
    Nil
  }

  def sacc(sym: SymbolInfoSymbol): s.Accessibility = {
    // TODO: Implement me.
    if (!sym.isParamAccessor && (sym.isPrivate || sym.isLocal)) s.Accessibility(a.PRIVATE)
    else if (sym.isProtected) s.Accessibility(a.PROTECTED)
    else s.Accessibility(a.PUBLIC)
  }

  def sowner(sym: SymbolInfoSymbol): String = {
    // TODO: Implement me.
    ""
  }

  private object ByNameType {
    def unapply(tpe: Type): Option[Type] = {
      tpe match {
        case TypeRefType(_, sym, List(tpe)) if sym.name == "<byname>" => Some(tpe)
        case _ => None
      }
    }
  }

  private object RepeatedType {
    def unapply(tpe: Type): Option[Type] = {
      tpe match {
        case TypeRefType(_, sym, List(tpe)) if sym.name == "<repeated>" => Some(tpe)
        case _ => None
      }
    }
  }

  private implicit class SymbolOps(sym: SymbolInfoSymbol) {
    def isModuleClass = sym.isInstanceOf[ClassSymbol] && sym.isModule
    def isType = sym.isInstanceOf[TypeSymbol]
    def isClassConstructor = {
      sym.parent match {
        case Some(parent: ClassSymbol) if !parent.isTrait && !parent.isModule =>
          sym.name == "<init>"
        case _ =>
          false
      }
    }
  }

  private implicit class TypeOps(tpe: Type) {
    def prefix = {
      tpe match {
        case TypeRefType(pre, _, _) => pre
        case SingleType(pre, _) => pre
        case _ => NoType
      }
    }
    def symbol = {
      tpe match {
        case TypeRefType(_, sym, _) => sym
        case SingleType(_, sym) => sym
        case ThisType(sym) => sym
        case _ => NoSymbol
      }
    }
    // TODO: Implement me.
    def hasNontrivialPrefix: Boolean = {
      val kind = skind(tpe.prefix.symbol)
      kind != k.OBJECT && kind != k.PACKAGE && kind != k.PACKAGE_OBJECT
    }
  }
}
