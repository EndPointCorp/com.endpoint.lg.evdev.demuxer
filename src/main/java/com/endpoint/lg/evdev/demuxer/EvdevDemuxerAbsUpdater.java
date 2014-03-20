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

import interactivespaces.util.concurrency.Updateable;

import com.google.common.eventbus.EventBus;

import com.endpoint.lg.support.evdev.InputEventHandler;

import com.endpoint.lg.support.evdev.InputEvent;
import com.endpoint.lg.support.evdev.InputAbsState;

/**
 * Aggregates EV_ABS events and publishes state updates.
 * 
 * @author Matt Vollrath <matt@endpoint.com>
 */
public class EvdevDemuxerAbsUpdater implements Updateable {
  private EventBus eventBus;
  private InputAbsState absState;

  public EvdevDemuxerAbsUpdater(EventBus eventBus) {
    this.eventBus = eventBus;

    absState = new InputAbsState();
  }

  /**
   * Publish a state update, if the state has changed since the last update.
   */
  @Override
  public void update() {
    if (absState.isDirty() || absState.isNonZero()) {
      eventBus.post(absState);
      absState.clean();
    }
  }

  /**
   * Generates a handler for updating the axis aggregator.
   */
  public InputEventHandler getHandler() {
    return new InputEventHandler() {
      public void handleEvent(InputEvent event) {
        absState.update(event);
      }
    };
  }
}
