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

package reactor.core.converter;

import java.util.concurrent.Flow;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

/**
 * Convert a Java 9+ {@literal Flow.Publisher} to/from a Reactive Streams {@link Publisher}.
 *
 * @author Stephane Maldini
 * @since 2.5
 */
@SuppressWarnings("rawtypes")
public final class FlowPublisherConverter extends PublisherConverter<Flow.Publisher> {

	static final FlowPublisherConverter INSTANCE = new FlowPublisherConverter();

	@SuppressWarnings("unchecked")
	static public <T> Flow.Publisher<T> from(Publisher<T> o) {
		return INSTANCE.fromPublisher(o);
	}

	@SuppressWarnings("unchecked")
	static public <T> Publisher<T> from(Flow.Publisher<T> o) {
		return INSTANCE.toPublisher(o);
	}

	@Override
	public Flow.Publisher fromPublisher(final Publisher<?> pub) {
		return new PublisherAsFlowPublisher(pub);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux toPublisher(Object o) {
		if (o instanceof Flow.Publisher) {
			final Flow.Publisher<?> pub = (Flow.Publisher<?>) o;
			return new FlowPublisherAsFlux(pub);
		}
		return null;
	}

	@Override
	public Class<Flow.Publisher> get() {
		return Flow.Publisher.class;
	}

	private final class FlowPublisherAsFlux extends Flux<Object> {
        private final java.util.concurrent.Flow.Publisher<?> pub;

        private FlowPublisherAsFlux(java.util.concurrent.Flow.Publisher<?> pub) {
            this.pub = pub;
        }

        @Override
        public void subscribe(final Subscriber<? super Object> s) {
        	pub.subscribe(new SubscriberToRS(s));
        }
    }

    private final class PublisherAsFlowPublisher implements Flow.Publisher<Object> {
        private final Publisher<?> pub;

        private PublisherAsFlowPublisher(Publisher<?> pub) {
            this.pub = pub;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super Object> subscriber) {
        	pub.subscribe(new FlowSubscriber(subscriber));
        }
    }

    private static class FlowSubscriber implements Subscriber<Object> {

		private final Flow.Subscriber<? super Object> subscriber;

		public FlowSubscriber(Flow.Subscriber<? super Object> subscriber) {
			this.subscriber = subscriber;
		}

		@Override
		public void onSubscribe(final Subscription s) {
			subscriber.onSubscribe(new Flow.Subscription() {
				@Override
				public void request(long l) {
					s.request(l);
				}

				@Override
				public void cancel() {
					s.cancel();
				}
			});
		}

		@Override
		public void onNext(Object o) {
			subscriber.onNext(o);
		}

		@Override
		public void onError(Throwable t) {
			subscriber.onError(t);
		}

		@Override
		public void onComplete() {
			subscriber.onComplete();
		}
	}

	private static class SubscriberToRS implements Flow.Subscriber<Object> {

		private final Subscriber<? super Object> s;

		public SubscriberToRS(Subscriber<? super Object> s) {
			this.s = s;
		}

		@Override
		public void onSubscribe(final Flow.Subscription subscription) {
			s.onSubscribe(new Subscription() {
				@Override
				public void request(long n) {
					subscription.request(n);
				}

				@Override
				public void cancel() {
					subscription.cancel();
				}
			});
		}

		@Override
		public void onNext(Object o) {
			s.onNext(o);
		}

		@Override
		public void onError(Throwable throwable) {
			s.onError(throwable);
		}

		@Override
		public void onComplete() {
			s.onComplete();
		}
	}
}
