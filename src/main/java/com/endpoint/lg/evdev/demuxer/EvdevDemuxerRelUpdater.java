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
import com.endpoint.lg.support.evdev.InputRelState;

/**
 * Aggregates EV_ABS events and publishes state updates.
 * 
 * @author Matt Vollrath <matt@endpoint.com>
 */
public class EvdevDemuxerRelUpdater implements Updateable {
  private EventBus eventBus;
  private InputRelState relState;
  private InputEventHandler handler;

  public EvdevDemuxerRelUpdater(EventBus eventBus) {
    this.eventBus = eventBus;

    relState = new InputRelState();

    handler = new InputEventHandler() {
      public void handleEvent(InputEvent event) {
        relState.update(event);
      }
    };
  }

  /**
   * Publish a state update, if the state has changed since the last update.
   * 
   * The state *must* be handled completely and synchronously by the receiving
   * handler, since it is cleared immediately after posting.
   */
  public void update() {
    if (relState.isDirty()) {
      eventBus.post(relState);
      relState.zero();
    }
  }

  /**
   * Generates a handler for updating the axis aggregator.
   */
  public InputEventHandler getHandler() {
    return handler;
  }
}
