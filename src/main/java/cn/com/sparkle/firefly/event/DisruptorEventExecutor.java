package cn.com.sparkle.firefly.event;

import cn.com.sparkle.firefly.event.events.Event;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;

public class DisruptorEventExecutor implements EventExecutor {
	private RingBuffer<WaitingEvent> ringBuffer;
	private Thread executeThread;
	public DisruptorEventExecutor() {
//		ringBuffer = RingBuffer.createMultiProducer(WaitingEvent.EVENT_FACTORY, 128, PhasedBackoffWaitStrategy.withLock(1, 1, TimeUnit.MILLISECONDS));
		ringBuffer = RingBuffer.createMultiProducer(WaitingEvent.EVENT_FACTORY, 8192, new BlockingWaitStrategy());
		
		BatchEventProcessor<WaitingEvent> processor = new BatchEventProcessor<WaitingEvent>(ringBuffer, ringBuffer.newBarrier(), new DisruptorHandler());
		ringBuffer.addGatingSequences(processor.getSequence());
		executeThread = new Thread(processor);
		executeThread.setName("disruptEventExecutor");
		executeThread.start();
	}

	@Override
	public void execute(Event event, Object... args) {
		if(Thread.currentThread() == executeThread){
			//directly invoke , because in the same thread maybe lead to deadlock
			event.notifyAllListener(args);
		}else{
			
			long seq = ringBuffer.next();
			WaitingEvent wEvent = ringBuffer.get(seq);
			wEvent.setArgs(args);
			wEvent.setEvent(event);
			ringBuffer.publish(seq);
		}
	}

	public final static class WaitingEvent {
		private Event event;
		private Object[] args;

		public Event getEvent() {
			return event;
		}

		public Object[] getArgs() {
			return args;
		}

		public void setEvent(Event event) {
			this.event = event;
		}

		public void setArgs(Object[] args) {
			this.args = args;
		}
		public void clear(){
			this.event = null;
			this.args = null;
		}
		
		public final static EventFactory<WaitingEvent> EVENT_FACTORY = new EventFactory<WaitingEvent>() {
			@Override
			public WaitingEvent newInstance() {
				return new WaitingEvent();
			}
		};
	}

	public class DisruptorHandler implements EventHandler<WaitingEvent> {
		@Override
		public void onEvent(WaitingEvent event, long sequence, boolean endOfBatch) throws Exception {
			event.getEvent().notifyAllListener(event.getArgs());
			event.clear();
		}
	}
}
