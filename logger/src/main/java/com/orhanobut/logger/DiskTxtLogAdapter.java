/**
 * Copyright 2017 Orhan Obut
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orhanobut.logger;

/**
 * Created by galois on 2017/11/20.
 */

public class DiskTxtLogAdapter implements LogAdapter {

    private final FormatStrategy formatStrategy;

    public DiskTxtLogAdapter() {
        formatStrategy = TxtFormatStrategy.newBuilder().build();
    }

    public DiskTxtLogAdapter(FormatStrategy formatStrategy) {
        this.formatStrategy = formatStrategy;
    }

    @Override public boolean isLoggable(int priority, String tag) {
        return true;
    }

    @Override public void log(int priority, String tag, String message) {
        formatStrategy.log(priority, tag, message);
    }
}