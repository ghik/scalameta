// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package scala.meta.internal.semanticdb

@SerialVersionUID(0L)
final case class Index(
    packages: _root_.scala.collection.Seq[scala.meta.internal.semanticdb.PackageEntry] = _root_.scala.collection.Seq.empty,
    toplevels: _root_.scala.collection.Seq[scala.meta.internal.semanticdb.ToplevelEntry] = _root_.scala.collection.Seq.empty
    ) extends scalapb.GeneratedMessage with scalapb.Message[Index] with scalapb.lenses.Updatable[Index] {
    @transient
    private[this] var __serializedSizeCachedValue: _root_.scala.Int = 0
    private[this] def __computeSerializedValue(): _root_.scala.Int = {
      var __size = 0
      packages.foreach { __item =>
        val __value = __item
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      }
      toplevels.foreach { __item =>
        val __value = __item
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      }
      __size
    }
    final override def serializedSize: _root_.scala.Int = {
      var read = __serializedSizeCachedValue
      if (read == 0) {
        read = __computeSerializedValue()
        __serializedSizeCachedValue = read
      }
      read
    }
    def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): _root_.scala.Unit = {
      packages.foreach { __v =>
        val __m = __v
        _output__.writeTag(1, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      toplevels.foreach { __v =>
        val __m = __v
        _output__.writeTag(2, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
    }
    def mergeFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): scala.meta.internal.semanticdb.Index = {
      val __packages = (_root_.scala.collection.immutable.Vector.newBuilder[scala.meta.internal.semanticdb.PackageEntry] ++= this.packages)
      val __toplevels = (_root_.scala.collection.immutable.Vector.newBuilder[scala.meta.internal.semanticdb.ToplevelEntry] ++= this.toplevels)
      var _done__ = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0 => _done__ = true
          case 10 =>
            __packages += _root_.scalapb.LiteParser.readMessage(_input__, scala.meta.internal.semanticdb.PackageEntry.defaultInstance)
          case 18 =>
            __toplevels += _root_.scalapb.LiteParser.readMessage(_input__, scala.meta.internal.semanticdb.ToplevelEntry.defaultInstance)
          case tag => _input__.skipField(tag)
        }
      }
      scala.meta.internal.semanticdb.Index(
          packages = __packages.result(),
          toplevels = __toplevels.result()
      )
    }
    def clearPackages = copy(packages = _root_.scala.collection.Seq.empty)
    def addPackages(__vs: scala.meta.internal.semanticdb.PackageEntry*): Index = addAllPackages(__vs)
    def addAllPackages(__vs: TraversableOnce[scala.meta.internal.semanticdb.PackageEntry]): Index = copy(packages = packages ++ __vs)
    def withPackages(__v: _root_.scala.collection.Seq[scala.meta.internal.semanticdb.PackageEntry]): Index = copy(packages = __v)
    def clearToplevels = copy(toplevels = _root_.scala.collection.Seq.empty)
    def addToplevels(__vs: scala.meta.internal.semanticdb.ToplevelEntry*): Index = addAllToplevels(__vs)
    def addAllToplevels(__vs: TraversableOnce[scala.meta.internal.semanticdb.ToplevelEntry]): Index = copy(toplevels = toplevels ++ __vs)
    def withToplevels(__v: _root_.scala.collection.Seq[scala.meta.internal.semanticdb.ToplevelEntry]): Index = copy(toplevels = __v)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => packages
        case 2 => toplevels
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PRepeated(packages.map(_.toPMessage)(_root_.scala.collection.breakOut))
        case 2 => _root_.scalapb.descriptors.PRepeated(toplevels.map(_.toPMessage)(_root_.scala.collection.breakOut))
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion = scala.meta.internal.semanticdb.Index
}

object Index extends scalapb.GeneratedMessageCompanion[scala.meta.internal.semanticdb.Index] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[scala.meta.internal.semanticdb.Index] = this
  def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, _root_.scala.Any]): scala.meta.internal.semanticdb.Index = {
    require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
    val __fields = javaDescriptor.getFields
    scala.meta.internal.semanticdb.Index(
      __fieldsMap.getOrElse(__fields.get(0), Nil).asInstanceOf[_root_.scala.collection.Seq[scala.meta.internal.semanticdb.PackageEntry]],
      __fieldsMap.getOrElse(__fields.get(1), Nil).asInstanceOf[_root_.scala.collection.Seq[scala.meta.internal.semanticdb.ToplevelEntry]]
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[scala.meta.internal.semanticdb.Index] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
      scala.meta.internal.semanticdb.Index(
        __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.collection.Seq[scala.meta.internal.semanticdb.PackageEntry]]).getOrElse(_root_.scala.collection.Seq.empty),
        __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.collection.Seq[scala.meta.internal.semanticdb.ToplevelEntry]]).getOrElse(_root_.scala.collection.Seq.empty)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = SemanticidxProto.javaDescriptor.getMessageTypes.get(0)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = SemanticidxProto.scalaDescriptor.messages(0)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[_] = null
    (__number: @_root_.scala.unchecked) match {
      case 1 => __out = scala.meta.internal.semanticdb.PackageEntry
      case 2 => __out = scala.meta.internal.semanticdb.ToplevelEntry
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = scala.meta.internal.semanticdb.Index(
  )
  implicit class IndexLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, scala.meta.internal.semanticdb.Index]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, scala.meta.internal.semanticdb.Index](_l) {
    def packages: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.collection.Seq[scala.meta.internal.semanticdb.PackageEntry]] = field(_.packages)((c_, f_) => c_.copy(packages = f_))
    def toplevels: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.collection.Seq[scala.meta.internal.semanticdb.ToplevelEntry]] = field(_.toplevels)((c_, f_) => c_.copy(toplevels = f_))
  }
  final val PACKAGES_FIELD_NUMBER = 1
  final val TOPLEVELS_FIELD_NUMBER = 2
}