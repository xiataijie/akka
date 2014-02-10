package akka.streams
package impl
package ops

import org.scalatest.{ FreeSpec, ShouldMatchers }
import Operation.{ FromIterableSource, Sink, Source }

class FlattenImplSpec extends FreeSpec with ShouldMatchers with SyncOperationSpec {
  "Flatten should" - {
    "initially" - {
      "request one substream as soon as at least one result element was requested" in new UnitializedSetup {
        flatten.handleRequestMore(1) should be(UpstreamRequestMore(1))
      }
    }
    "while consuming substream" - {
      "request elements from substream and deliver results to downstream" in new UnitializedSetup {
        flatten.handleRequestMore(10) should be(UpstreamRequestMore(1))
        flatten.handleRequestMore(1) should be(Continue) // don't request more substreams while one is still pending
        val SubscribeTo(CustomSource, onSubscribe) = flatten.handleNext(CustomSource)
        val (subDownstream, res) = onSubscribe(SubUpstream)
        res should be(RequestMoreFromSubstream(11))
        subDownstream.handleNext(1.5f) should be(DownstreamNext(1.5f))
        subDownstream.handleNext(8.7f) should be(DownstreamNext(8.7f))
      }
      "go on with super stream when substream is depleted" in new UnitializedSetup {
        flatten.handleRequestMore(10) should be(UpstreamRequestMore(1))
        flatten.handleRequestMore(1) should be(Continue) // don't request more substreams while one is still pending
        val SubscribeTo(CustomSource, onSubscribe) = flatten.handleNext(CustomSource)
        val (subDownstream, res) = onSubscribe(SubUpstream)
        res should be(RequestMoreFromSubstream(11))
        subDownstream.handleComplete() should be(UpstreamRequestMore(1))
      }
      "eventually close to downstream when super stream closes and last substream  is depleted" in new UnitializedSetup {
        flatten.handleRequestMore(10) should be(UpstreamRequestMore(1))
        flatten.handleRequestMore(1) should be(Continue) // don't request more substreams while one is still pending
        val SubscribeTo(CustomSource, onSubscribe) = flatten.handleNext(CustomSource)
        val (subDownstream, res) = onSubscribe(SubUpstream)
        res should be(RequestMoreFromSubstream(11))
        subDownstream.handleNext(1.5f) should be(DownstreamNext(1.5f))
        flatten.handleComplete() should be(Continue)
        subDownstream.handleNext(8.3f) should be(DownstreamNext(8.3f))
        subDownstream.handleComplete() should be(DownstreamComplete)
      }
      "deliver many elements from substream to output" in pending
      "cancel substream and main stream when cancelled itself" in pending
      "immediately report substream error" in pending
    }
  }

  class UnitializedSetup {
    val CustomSource = FromIterableSource(Seq(1f, 2f, 3f))
    case class SubscribeTo[O](source: Source[O], onSubscribe: Upstream ⇒ (SyncSink[O], Effect)) extends SideEffect {
      def run(): Unit = ???
    }
    case class RequestMoreFromSubstream(n: Int) extends SideEffect {
      def run(): Unit = ???
    }
    case object CancelSubstream extends SideEffect {
      def run(): Unit = ???
    }
    object SubUpstream extends Upstream {
      val requestMore: Int ⇒ Effect = RequestMoreFromSubstream
      val cancel: Effect = CancelSubstream
    }
    val ctx = new ContextEffects {
      def subscribeTo[O](source: Source[O])(onSubscribe: Upstream ⇒ (SyncSink[O], Effect)): Effect =
        SubscribeTo(source, onSubscribe)

      def subscribeFrom[O](sink: Sink[O])(onSubscribe: (Downstream[O]) ⇒ (SyncSource, Effect)): Effect = ???
    }
    val flatten = FlattenImpl(upstream, downstream, ctx)
  }
}
