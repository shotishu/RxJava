/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.concurrency.TestScheduler;
import rx.subjects.PublishSubject;
import rx.util.TimeInterval;

/**
 * Records the time interval between consecutive elements in an observable sequence.
 */
public class OperationTimeInterval {

    public static <T> OnSubscribeFunc<TimeInterval<T>> timeInterval(
            Observable<? extends T> source) {
        return timeInterval(source, Schedulers.immediate());
    }

    public static <T> OnSubscribeFunc<TimeInterval<T>> timeInterval(
            final Observable<? extends T> source, final Scheduler scheduler) {
        return new OnSubscribeFunc<TimeInterval<T>>() {
            @Override
            public Subscription onSubscribe(
                    Observer<? super TimeInterval<T>> observer) {
                return source.subscribe(new TimeIntervalObserver<T>(observer,
                        scheduler));
            }
        };
    }

    private static class TimeIntervalObserver<T> implements Observer<T> {

        private final Observer<? super TimeInterval<T>> observer;
        /**
         * Only used to compute time intervals.
         */
        private final Scheduler scheduler;
        private long lastTimestamp;

        public TimeIntervalObserver(Observer<? super TimeInterval<T>> observer,
                Scheduler scheduler) {
            this.observer = observer;
            this.scheduler = scheduler;
            // The beginning time is the time when the observer subscribes.
            lastTimestamp = scheduler.now();
        }

        @Override
        public void onNext(T args) {
            long nowTimestamp = scheduler.now();
            observer.onNext(new TimeInterval<T>(nowTimestamp - lastTimestamp,
                    args));
            lastTimestamp = nowTimestamp;
        }

        @Override
        public void onCompleted() {
            observer.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            observer.onCompleted();
        }
    }

    public static class UnitTest {

        private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

        @Mock
        private Observer<TimeInterval<Integer>> observer;

        private TestScheduler testScheduler;
        private PublishSubject<Integer> subject;
        private Observable<TimeInterval<Integer>> observable;

        @Before
        public void setUp() {
            MockitoAnnotations.initMocks(this);
            testScheduler = new TestScheduler();
            subject = PublishSubject.create();
            observable = subject.timeInterval(testScheduler);
        }

        @Test
        public void testTimeInterval() {
            InOrder inOrder = inOrder(observer);
            observable.subscribe(observer);

            testScheduler.advanceTimeBy(1000, TIME_UNIT);
            subject.onNext(1);
            testScheduler.advanceTimeBy(2000, TIME_UNIT);
            subject.onNext(2);
            testScheduler.advanceTimeBy(3000, TIME_UNIT);
            subject.onNext(3);
            subject.onCompleted();

            inOrder.verify(observer, times(1)).onNext(
                    new TimeInterval<Integer>(1000, 1));
            inOrder.verify(observer, times(1)).onNext(
                    new TimeInterval<Integer>(2000, 2));
            inOrder.verify(observer, times(1)).onNext(
                    new TimeInterval<Integer>(3000, 3));
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }
    }

}
