package com.endpoint.lg.evdev.demuxer.test;

import static org.junit.Assert.*;
import interactivespaces.util.data.json.JsonNavigator;

import org.junit.BeforeClass;
import org.junit.Test;

import com.endpoint.lg.evdev.demuxer.EvdevDemuxerRelUpdater;
import com.endpoint.lg.support.evdev.InputRelState;
import com.endpoint.lg.support.evdev.InputEvent;
import com.endpoint.lg.support.evdev.InputEventHandler;

import static com.endpoint.lg.support.evdev.InputEventTypes.*;
import static com.endpoint.lg.support.evdev.InputEventCodes.*;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class TestEvdevDemuxerRelUpdater {
  private static final int TEST_REL_X_TYPE = EV_REL;
  private static final int TEST_REL_X_CODE = REL_X;
  private static final int TEST_REL_X_VALUE_0 = 12;
  private static final int TEST_REL_X_VALUE_1 = 21;

  private static InputEvent testEvent0;
  private static InputEvent testEvent1;

  @BeforeClass
  public static void testSetup() throws Exception {
    testEvent0 = new InputEvent(TEST_REL_X_TYPE, TEST_REL_X_CODE, TEST_REL_X_VALUE_0);
    testEvent1 = new InputEvent(TEST_REL_X_TYPE, TEST_REL_X_CODE, TEST_REL_X_VALUE_1);
  }

  /**
   * A mock class for catching InputRelState events.
   * 
   * @author Matt Vollrath <matt@endpoint.com>
   */
  private class MockRelStateBus {
    private EventBus eventBus;
    private InputRelState result;

    public EventBus getEventBus() {
      return eventBus;
    }

    public InputRelState flushResult() {
      InputRelState flushed = result;
      result = null;
      return flushed;
    }

    // copy the state to avoid timing issues
    @Subscribe
    public void onUpdate(InputRelState state) {
      JsonNavigator copy = new JsonNavigator(state.getJsonBuilder().build());

      result = new InputRelState(copy);
    }

    public MockRelStateBus() {
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
    InputRelState result;
    MockRelStateBus mockBus = new MockRelStateBus();

    EvdevDemuxerRelUpdater updater = new EvdevDemuxerRelUpdater(mockBus.getEventBus());

    updater.update();
    result = mockBus.flushResult();

    assertNull(result);
  }

  /**
   * Verify that an updater with non-zero state publishes events.
   */
  @Test
  public void testDirtyUpdates() {
    InputRelState result;
    MockRelStateBus mockBus = new MockRelStateBus();

    EvdevDemuxerRelUpdater updater = new EvdevDemuxerRelUpdater(mockBus.getEventBus());

    InputEventHandler handler = updater.getHandler();

    handler.handleEvent(testEvent0);

    updater.update();
    result = mockBus.flushResult();

    assertEquals(TEST_REL_X_VALUE_0, result.getValue(TEST_REL_X_CODE));

    // update again to verify that the state is clear
    updater.update();
    result = mockBus.flushResult();

    assertNull(result);

    // handle a different event and update again
    handler.handleEvent(testEvent1);

    updater.update();
    result = mockBus.flushResult();

    assertEquals(TEST_REL_X_VALUE_1, result.getValue(TEST_REL_X_CODE));
  }
}
