/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.core.publisher.tck;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Processor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.TopicProcessor;
import reactor.core.publisher.WorkQueueProcessor;
import reactor.core.subscriber.Subscribers;
import reactor.core.test.TestSubscriber;
import reactor.core.util.Exceptions;

/**
 * @author Stephane Maldini
 */
@org.testng.annotations.Test
public class WorkQueueProcessorTests extends AbstractProcessorVerification {

	@Override
	public Processor<Long, Long> createProcessor(int bufferSize) {
		System.out.println("new processor");
		return  WorkQueueProcessor.create("rb-work", bufferSize);
	}

	@Override
	public void required_mustRequestFromUpstreamForElementsThatHaveBeenRequestedLongAgo()
			throws Throwable {
		//IGNORE since subscribers see distinct data
	}

	@Override
	public void required_spec104_mustCallOnErrorOnAllItsSubscribersIfItEncountersANonRecoverableError()
			throws Throwable {
		super.required_spec104_mustCallOnErrorOnAllItsSubscribersIfItEncountersANonRecoverableError();
	}

	@Test
	public void drainTest() throws Exception {
		final TopicProcessor<Integer> sink = TopicProcessor.create("topic");
		sink.onNext(1);
		sink.onNext(2);
		sink.onNext(3);

		TestSubscriber.subscribe(sink.forceShutdown())
		                             .assertComplete()
		                             .assertValues(1, 2, 3);
	}

	@Override
	public void simpleTest() throws Exception {
		final TopicProcessor<Integer> sink = TopicProcessor.create("topic");
		final WorkQueueProcessor<Integer> processor = WorkQueueProcessor.create("queue");

		int elems = 1_000_000;
		CountDownLatch latch = new CountDownLatch(elems);

		//List<Integer> list = new CopyOnWriteArrayList<>();
		AtomicLong count = new AtomicLong();
		AtomicLong errorCount = new AtomicLong();

		processor.subscribe(Subscribers.unbounded((d, sub) -> {
			errorCount.incrementAndGet();
			throw Exceptions.failWithCancel();
		}));

		Flux.from(processor).doOnNext(
			d -> count.incrementAndGet()
		).subscribe(Subscribers.unbounded((d, sub) -> {
			latch.countDown();
			//list.add(d);
		}));

		sink.subscribe(processor);
		sink.connect();
		for(int i = 0; i < elems; i++){

			sink.onNext(i);
			if( i % 100 == 0) {
				processor.subscribe(Subscribers.unbounded((d, sub) -> {
					errorCount.incrementAndGet();
					throw Exceptions.failWithCancel();
				}));
			}
		}

		latch.await(5, TimeUnit.SECONDS);
		System.out.println("count " + count+" errors: "+errorCount);
		sink.onComplete();
		Assert.assertTrue("Latch is " + latch.getCount(), latch.getCount() <= 1);
	}

	@Override
	public void mustImmediatelyPassOnOnErrorEventsReceivedFromItsUpstreamToItsDownstream() throws Exception {
		super.mustImmediatelyPassOnOnErrorEventsReceivedFromItsUpstreamToItsDownstream();
	}

}
