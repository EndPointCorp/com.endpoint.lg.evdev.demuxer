/*
 * Copyright (C) 2015 End Point Corporation
 * Copyright (C) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.endpoint.lg.evdev.demuxer;

import static org.junit.Assert.*;
import interactivespaces.util.data.json.JsonNavigator;

import org.junit.BeforeClass;
import org.junit.Test;

import com.endpoint.lg.evdev.demuxer.EvdevDemuxerAbsUpdater;
import com.endpoint.lg.support.evdev.InputAbsState;
import com.endpoint.lg.support.evdev.InputEvent;
import com.endpoint.lg.support.evdev.InputEventHandler;

import static com.endpoint.lg.support.evdev.InputEventTypes.*;
import static com.endpoint.lg.support.evdev.InputEventCodes.*;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Test <code>EvdevDemuxerAbsUpdater</code>.
 * 
 * @author Matt Vollrath <matt@endpoint.com>
 */
public class TestEvdevDemuxerAbsUpdater {
  private static final int TEST_ABS_X_TYPE = EV_ABS;
  private static final int TEST_ABS_X_CODE = ABS_X;
  private static final int TEST_ABS_X_VALUE_0 = 12;
  private static final int TEST_ABS_X_VALUE_1 = 21;

  private static InputEvent testEvent0;
  private static InputEvent testEvent1;

  @BeforeClass
  public static void testSetup() throws Exception {
    testEvent0 = new InputEvent(TEST_ABS_X_TYPE, TEST_ABS_X_CODE, TEST_ABS_X_VALUE_0);
    testEvent1 = new InputEvent(TEST_ABS_X_TYPE, TEST_ABS_X_CODE, TEST_ABS_X_VALUE_1);
  }

  /**
   * A mock class for catching InputAbsState events.
   * 
   * @author Matt Vollrath <matt@endpoint.com>
   */
  private class MockAbsStateBus {
    private EventBus eventBus;
    private InputAbsState result;

    public EventBus getEventBus() {
      return eventBus;
    }

    public InputAbsState flushResult() {
      InputAbsState flushed = result;
      result = null;
      return flushed;
    }

    // copy the state to avoid timing issues
    @Subscribe
    public void onUpdate(InputAbsState state) {
      JsonNavigator copy = new JsonNavigator(state.getJsonBuilder().build());

      result = new InputAbsState(copy);
    }

    public MockAbsStateBus() {
      result = null;

      eventBus = new EventBus();
      eventBus.register(this);
    }
  }

  /**
   * Verify that an updater with no state does not publish events.
   */
  @Test
  public void testCleanUpdate() {
    InputAbsState result;
    MockAbsStateBus mockBus = new MockAbsStateBus();

    EvdevDemuxerAbsUpdater updater = new EvdevDemuxerAbsUpdater(mockBus.getEventBus());

    updater.update();
    result = mockBus.flushResult();

    assertNull(result);
  }

  /**
   * Verify that an updater with non-zero state publishes events.
   */
  @Test
  public void testDirtyUpdates() {
    InputAbsState result;
    MockAbsStateBus mockBus = new MockAbsStateBus();

    EvdevDemuxerAbsUpdater updater = new EvdevDemuxerAbsUpdater(mockBus.getEventBus());

    InputEventHandler handler = updater.getHandler();

    handler.handleEvent(testEvent0);

    updater.update();
    result = mockBus.flushResult();

    assertEquals(TEST_ABS_X_VALUE_0, result.getValue(TEST_ABS_X_CODE));

    // update again to verify that the state is still published
    updater.update();
    result = mockBus.flushResult();

    assertEquals(TEST_ABS_X_VALUE_0, result.getValue(TEST_ABS_X_CODE));

    // handle a different event and update again
    handler.handleEvent(testEvent1);

    updater.update();
    result = mockBus.flushResult();

    assertEquals(TEST_ABS_X_VALUE_1, result.getValue(TEST_ABS_X_CODE));
  }
}
