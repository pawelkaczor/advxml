package advxml.core.transform

import advxml.core.transform.actions.{ComposableXmlModifier, FinalXmlModifier, XmlModifier}
import advxml.core.validate.MonadEx

import scala.xml.NodeSeq
import scala.xml.transform.{BasicTransformer, RewriteRule, RuleTransformer}

object XmlTransformer {

  def transform[F[_]: MonadEx](root: NodeSeq, modifier: XmlModifier): F[NodeSeq] = modifier match {
    case m: FinalXmlModifier      => transform(root, Seq(PartialXmlRule(identity).withModifier(m)))
    case m: ComposableXmlModifier => transform(root, Seq(PartialXmlRule(identity).withModifier(m)))
  }

  def transform[F[_]: MonadEx](root: NodeSeq, rules: Seq[XmlRule]): F[NodeSeq] =
    transform(new RuleTransformer(_: _*))(root, rules)

  def transform[F[_]: MonadEx](
    f: Seq[RewriteRule] => BasicTransformer
  )(root: NodeSeq, rules: Seq[XmlRule]): F[NodeSeq] = {

    import cats.implicits._

    rules
      .map(_.toRewriteRule[F](root))
      .toList
      .sequence
      .map(f(_).transform(root))
  }
}