/*
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

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.util.concurrency.UpdateableLoop;
import interactivespaces.util.data.json.JsonNavigator;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import com.endpoint.lg.support.evdev.InputRelState;

import com.endpoint.lg.support.evdev.InputAbsState;

import java.util.Map;

import com.endpoint.lg.support.evdev.InputEvent;
import com.endpoint.lg.support.evdev.InputEventHandler;
import com.endpoint.lg.support.evdev.InputEventHandlers;
import com.endpoint.lg.support.evdev.InputEventTypes;
import com.endpoint.lg.support.evdev.InputKeyEvent;

/**
 * An activity for separating and aggregating raw user input events.
 * 
 * @author Matt Vollrath <matt@endpoint.com>
 */
public class EvdevDemuxerActivity extends BaseRoutableRosActivity {

  /**
   * Each axis is serialized and published at this frequency (in Hz).
   */
  public static final double REFRESH_RATE = 60.0;

  private InputEventHandlers inputHandlers;

  @Override
  public void onNewInputJson(String channelName, Map<String, Object> message) {
    JsonNavigator json = new JsonNavigator(message);

    inputHandlers.handleEvent(new InputEvent(json));
  }

  /**
   * Handles EV_KEY events.
   * 
   * @param keyEvent
   *          the key event
   */
  @Subscribe
  public void onKeyUpdate(InputKeyEvent keyEvent) {
    getLog().info("sending key event");
    sendOutputJsonBuilder("key", InputKeyEvent.serialize(keyEvent));
  }

  /**
   * Handles EV_ABS state updates.
   * 
   * @param absState
   *          the updated state
   */
  @Subscribe
  public void onAbsUpdate(InputAbsState absState) {
    getLog().info("sending abs update");
    sendOutputJsonBuilder("abs", absState.getNonZeroAsJsonBuilder());
  }

  /**
   * Handles EV_REL state updates.
   * 
   * @param relState
   *          the updated state
   */
  @Subscribe
  public void onRelUpdate(InputRelState relState) {
    getLog().info("sending rel update");
    sendOutputJsonBuilder("rel", relState.getDirtyAsJsonBuilder());
  }

  @Override
  public void onActivitySetup() {
    final EventBus eventBus = new EventBus();
    eventBus.register(this);

    final EvdevDemuxerAbsUpdater absUpdater = new EvdevDemuxerAbsUpdater(eventBus);

    UpdateableLoop absLoop = new UpdateableLoop(absUpdater);
    absLoop.setFrameRate(REFRESH_RATE);
    getManagedCommands().submit(absLoop);

    final EvdevDemuxerRelUpdater relUpdater = new EvdevDemuxerRelUpdater(eventBus);

    UpdateableLoop relLoop = new UpdateableLoop(relUpdater);
    relLoop.setFrameRate(REFRESH_RATE);
    getManagedCommands().submit(relLoop);

    inputHandlers = new InputEventHandlers();

    inputHandlers.registerHandler(InputEventTypes.EV_KEY, InputEventHandlers.ALL_CODES,
        new InputEventHandler() {
          public void handleEvent(InputEvent event) {
            eventBus.post(new InputKeyEvent(event));
          }
        });

    inputHandlers.registerHandler(InputEventTypes.EV_ABS, InputEventHandlers.ALL_CODES,
        absUpdater.getHandler());

    inputHandlers.registerHandler(InputEventTypes.EV_REL, InputEventHandlers.ALL_CODES,
        relUpdater.getHandler());
  }
}
