package japgolly.scalajs.react.component.builder

import org.scalajs.dom
import japgolly.scalajs.react.{Callback, CallbackTo, PropsChildren}
import japgolly.scalajs.react.component.Scala._
import japgolly.scalajs.react.internal._
import Lifecycle._

final case class Lifecycle[P, S, B](
  componentDidMount        : Option[ComponentDidMountFn        [P, S, B]],
  componentDidUpdate       : Option[ComponentDidUpdateFn       [P, S, B]],
  componentWillMount       : Option[ComponentWillMountFn       [P, S, B]],
  componentWillReceiveProps: Option[ComponentWillReceivePropsFn[P, S, B]],
  componentWillUnmount     : Option[ComponentWillUnmountFn     [P, S, B]],
  componentWillUpdate      : Option[ComponentWillUpdateFn      [P, S, B]],
  shouldComponentUpdate    : Option[ShouldComponentUpdateFn    [P, S, B]]) {

  type This = Lifecycle[P, S, B]

  def append[I, O](lens: Lens[Lifecycle[P, S, B], Option[I => O]])(g: I => O)(implicit s: Semigroup[O]): This =
    lens.mod(o => Some(o.fold(g)(f => i => s.append(f(i), g(i)))))(this)
}

object Lifecycle {
  def empty[P, S, B]: Lifecycle[P, S, B] =
    new Lifecycle(None, None, None, None, None, None, None)

  sealed trait Base[P, S, B] extends Any {
    def raw: RawMounted[P, S, B]

    final def backend      : B                      = raw.backend
    final def mountedImpure: MountedImpure[P, S, B] = raw.mountedImpure
    final def mountedPure  : MountedPure[P, S, B]   = raw.mountedPure
  }

  sealed trait StateW[P, S, B] extends Any with Base[P, S, B] {
    final def setState(newState: S, cb: Callback = Callback.empty): Callback = mountedPure.setState(newState, cb)
    final def modState(mod: S => S, cb: Callback = Callback.empty): Callback = mountedPure.modState(mod, cb)

    final def setStateFn[I](f: I => S, callback: Callback = Callback.empty): I => Callback =
      i => setState(f(i), callback)

    final def modStateFn[I](f: I => S => S, callback: Callback = Callback.empty): I => Callback =
      i => modState(f(i), callback)
  }

  sealed trait StateRW[P, S, B] extends Any with StateW[P, S, B] {
    final def state: S = mountedImpure.state
  }

  sealed trait ForceUpdate[P, S, B] extends Any with Base[P, S, B] {
    final def forceUpdate: Callback = forceUpdate(Callback.empty)
    final def forceUpdate(cb: Callback): Callback = mountedPure.forceUpdate(cb)
  }

  private def wrapTostring(toString: String) = toString
    .replaceAll("undefined → undefined", "undefined")
    .replace("props" +
      ": undefined, ", "")
    .replace("state: undefined)", ")")
    .replace(", )", ")")

  // ===================================================================================================================

  def componentDidMount[P, S, B] = Lens((_: Lifecycle[P, S, B]).componentDidMount)(n => _.copy(componentDidMount = n))

  type ComponentDidMountFn[P, S, B] = ComponentDidMount[P, S, B] => Callback

  final class ComponentDidMount[P, S, B](val raw: RawMounted[P, S, B])
      extends AnyVal with StateRW[P, S, B] with ForceUpdate[P, S, B] {

    override def toString = wrapTostring(s"ComponentDidMount(props: $props, state: $state)")

    def props        : P                = mountedImpure.props
    def propsChildren: PropsChildren    = mountedImpure.propsChildren
    def getDOMNode   : dom.Element      = mountedImpure.getDOMNode
  }

  // ===================================================================================================================

  def componentDidUpdate[P, S, B] = Lens((_: Lifecycle[P, S, B]).componentDidUpdate)(n => _.copy(componentDidUpdate = n))

  type ComponentDidUpdateFn[P, S, B] = ComponentDidUpdate[P, S, B] => Callback

  final class ComponentDidUpdate[P, S, B](val raw: RawMounted[P, S, B], val prevProps: P, val prevState: S)
      extends StateW[P, S, B] with ForceUpdate[P, S, B] {

    override def toString = wrapTostring(s"ComponentDidUpdate(props: $prevProps → $currentProps, state: $prevState → $currentState)")

    def propsChildren: PropsChildren = mountedImpure.propsChildren
    def currentProps : P             = mountedImpure.props
    def currentState : S             = mountedImpure.state
    def getDOMNode   : dom.Element   = mountedImpure.getDOMNode
  }

  // ===================================================================================================================

  def componentWillMount[P, S, B] = Lens((_: Lifecycle[P, S, B]).componentWillMount)(n => _.copy(componentWillMount = n))

  type ComponentWillMountFn[P, S, B] = ComponentWillMount[P, S, B] => Callback

  final class ComponentWillMount[P, S, B](val raw: RawMounted[P, S, B])
      extends AnyVal with StateRW[P, S, B] {

    override def toString = wrapTostring(s"ComponentWillMount(props: $props, state: $state)")

    def props        : P             = mountedImpure.props
    def propsChildren: PropsChildren = mountedImpure.propsChildren

    @deprecated("forceUpdate prohibited within the componentWillMount callback.", "")
    def forceUpdate(prohibited: Nothing = ???): Nothing = ???

    // Nope
    // def getDOMNode   : dom.Element   = raw.mounted.getDOMNode
  }

  // ===================================================================================================================

  def componentWillUnmount[P, S, B] = Lens((_: Lifecycle[P, S, B]).componentWillUnmount)(n => _.copy(componentWillUnmount = n))

  type ComponentWillUnmountFn[P, S, B] = ComponentWillUnmount[P, S, B] => Callback

  final class ComponentWillUnmount[P, S, B](val raw: RawMounted[P, S, B])
      extends AnyVal with Base[P, S, B] {

    override def toString = wrapTostring(s"ComponentWillUnmount(props: $props, state: $state)")

    def props        : P             = mountedImpure.props
    def propsChildren: PropsChildren = mountedImpure.propsChildren
    def state        : S             = mountedImpure.state
    def getDOMNode   : dom.Element   = mountedImpure.getDOMNode

    @deprecated("setState prohibited within the componentWillUnmount callback.", "")
    def setState(prohibited: Nothing, cb: Callback = ???): Nothing = ???

    @deprecated("modState prohibited within the componentWillUnmount callback.", "")
    def modState(prohibited: Nothing, cb: Callback = ???): Nothing = ???

    @deprecated("forceUpdate prohibited within the componentWillUnmount callback.", "")
    def forceUpdate(prohibited: Nothing = ???): Nothing = ???
  }

  // ===================================================================================================================

  def componentWillReceiveProps[P, S, B] = Lens((_: Lifecycle[P, S, B]).componentWillReceiveProps)(n => _.copy(componentWillReceiveProps = n))

  type ComponentWillReceivePropsFn[P, S, B] = ComponentWillReceiveProps[P, S, B] => Callback

  final class ComponentWillReceiveProps[P, S, B](val raw: RawMounted[P, S, B], val nextProps: P)
      extends StateRW[P, S, B] with ForceUpdate[P, S, B] {

    override def toString = wrapTostring(s"ComponentWillReceiveProps(props: $currentProps → $nextProps, state: $state)")

    def propsChildren: PropsChildren = mountedImpure.propsChildren
    def currentProps : P             = mountedImpure.props
    def getDOMNode   : dom.Element   = mountedImpure.getDOMNode
  }

  // ===================================================================================================================

  def componentWillUpdate[P, S, B] = Lens((_: Lifecycle[P, S, B]).componentWillUpdate)(n => _.copy(componentWillUpdate = n))

  type ComponentWillUpdateFn[P, S, B] = ComponentWillUpdate[P, S, B] => Callback

  final class ComponentWillUpdate[P, S, B](val raw: RawMounted[P, S, B], val nextProps: P, val nextState: S)
      extends Base[P, S, B] {

    override def toString = wrapTostring(s"ComponentWillUpdate(props: $currentProps → $nextProps, state: $currentState → $nextState)")

    def propsChildren: PropsChildren = mountedImpure.propsChildren
    def currentProps : P             = mountedImpure.props
    def currentState : S             = mountedImpure.state
    def getDOMNode   : dom.Element   = mountedImpure.getDOMNode

    @deprecated("setState prohibited within the componentWillUpdate callback. Use componentWillReceiveProps instead.", "")
    def setState(prohibited: Nothing, cb: Callback = ???): Nothing = ???

    @deprecated("modState prohibited within the componentWillUpdate callback. Use componentWillReceiveProps instead.", "")
    def modState(prohibited: Nothing, cb: Callback = ???): Nothing = ???

    @deprecated("forceUpdate prohibited within the componentWillUpdate callback. Use componentWillReceiveProps instead.", "")
    def forceUpdate(prohibited: Nothing = ???): Nothing = ???
  }

  // ===================================================================================================================

  def shouldComponentUpdate[P, S, B] = Lens((_: Lifecycle[P, S, B]).shouldComponentUpdate)(n => _.copy(shouldComponentUpdate = n))

  type ShouldComponentUpdateFn[P, S, B] = ShouldComponentUpdate[P, S, B] => CallbackTo[Boolean]

  final class ShouldComponentUpdate[P, S, B](val raw: RawMounted[P, S, B], val nextProps: P, val nextState: S)
      extends Base[P, S, B] {

    override def toString = wrapTostring(s"ShouldComponentUpdate(props: $currentProps → $nextProps, state: $currentState → $nextState)")

    def propsChildren: PropsChildren = mountedImpure.propsChildren
    def currentProps : P             = mountedImpure.props
    def currentState : S             = mountedImpure.state
    def getDOMNode   : dom.Element   = mountedImpure.getDOMNode

    def cmpProps(cmp: (P, P) => Boolean): Boolean = cmp(currentProps, nextProps)
    def cmpState(cmp: (S, S) => Boolean): Boolean = cmp(currentState, nextState)

    @deprecated("setState prohibited within the shouldComponentUpdate callback.", "")
    def setState(prohibited: Nothing, cb: Callback = ???): Nothing = ???

    @deprecated("modState prohibited within the shouldComponentUpdate callback.", "")
    def modState(prohibited: Nothing, cb: Callback = ???): Nothing = ???

    @deprecated("forceUpdate prohibited within the shouldComponentUpdate callback.", "")
    def forceUpdate(prohibited: Nothing = ???): Nothing = ???
  }

  // ===================================================================================================================

  final class RenderScope[P, S, B](val raw: RawMounted[P, S, B])
      extends AnyVal with StateRW[P, S, B] with ForceUpdate[P, S, B] {

    override def toString = wrapTostring(s"Render(props: $props, state: $state)")

    def props        : P             = mountedImpure.props
    def propsChildren: PropsChildren = mountedImpure.propsChildren
    def getDOMNode   : dom.Element   = mountedImpure.getDOMNode
  }

}
