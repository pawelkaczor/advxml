package advxml.core.transform

import advxml.core.MonadEx
import advxml.core.utils.XmlUtils
import cats.Monoid

import scala.xml.{Elem, NodeSeq}

/** advxml
  * Created by geirolad on 09/06/2019.
  *
  * @author geirolad
  */
sealed trait XmlRule extends AbstractRule {
  val zoom: XmlZoom
}

sealed trait ComposableXmlRule extends XmlRule {
  val modifiers: List[ComposableXmlModifier]
  def withModifier(modifier: ComposableXmlModifier): ComposableXmlRule
}
sealed trait FinalXmlRule extends XmlRule {
  val modifier: FinalXmlModifier
}

object XmlRule {

  import advxml.instances.transform.composableXmlModifierMonoidInstance
  import cats.syntax.all._

  private[transform] def transform[F[_]](root: NodeSeq, rule: XmlRule)(implicit F: MonadEx[F]): F[NodeSeq] = {
    val modifier = rule match {
      case r: ComposableXmlRule => Monoid.combineAll(r.modifiers)
      case r: FinalXmlRule      => r.modifier
    }
    buildRewriteRule(root, rule.zoom, modifier)
  }

  private def buildRewriteRule[F[_]](root: NodeSeq, zoom: XmlZoom, modifier: XmlModifier)(implicit
    F: MonadEx[F]
  ): F[NodeSeq] = {
    for {
      target <- zoom.detailed[F](root)
      targetNodeSeq = target.nodeSeq
      targetParents = target.parents
      updatedTarget <- modifier[F](targetNodeSeq)
      updatedWholeDocument = {
        targetParents
          .foldRight(XmlPatch.const(targetNodeSeq, updatedTarget))((parent, patch) =>
            XmlPatch(
              parent,
              _.flatMap { case e: Elem =>
                XmlUtils.flatMapChildren(
                  e,
                  n =>
                    patch.zipWithUpdated
                      .getOrElse(Some(n), Some(n))
                      .getOrElse(NodeSeq.Empty)
                )
              }
            )
          )
          .updated
      }
    } yield updatedWholeDocument
  }

  //============================== BUILD ==============================
  def apply(
    zoom: XmlZoom,
    modifier1: ComposableXmlModifier,
    modifiers: ComposableXmlModifier*
  ): ComposableXmlRule =
    Impls.Composable(zoom, (modifier1 +: modifiers).toList)

  def apply(zoom: XmlZoom, modifiers: List[ComposableXmlModifier]): ComposableXmlRule =
    Impls.Composable(zoom, modifiers)

  def apply(zoom: XmlZoom, modifier: FinalXmlModifier): FinalXmlRule =
    Impls.Final(zoom, modifier)

  //============================== IMPLS ==============================
  private object Impls {

    case class Composable(zoom: XmlZoom, modifiers: List[ComposableXmlModifier]) extends ComposableXmlRule {

      override def withModifier(modifier: ComposableXmlModifier): ComposableXmlRule =
        copy(modifiers = modifiers :+ modifier)
    }

    case class Final(zoom: XmlZoom, modifier: FinalXmlModifier) extends FinalXmlRule

  }
}
