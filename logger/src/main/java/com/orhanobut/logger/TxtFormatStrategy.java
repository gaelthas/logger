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

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by galois on 2017/11/20.
 */

public class TxtFormatStrategy implements FormatStrategy {
    private static final String NEW_LINE = "\r\n";
    private static final String SEPARATOR = " , ";

    /**
     * Android's max limit for a log entry is ~4076 bytes,
     * so 4000 bytes is used as chunk size since default charset
     * is UTF-8
     */
    private static final int CHUNK_SIZE = 4000;
    /**
     * The minimum stack trace index, starts at this class after two native calls.
     */
    private static final int MIN_STACK_OFFSET = 5;

    private final int methodCount;
    private final int methodOffset;
    private final boolean showThreadInfo;

    private final Date date;
    private final SimpleDateFormat dateFormat;
    private final LogStrategy logStrategy;
    private final String tag;

    private TxtFormatStrategy(Builder builder) {
        date = builder.date;
        dateFormat = builder.dateFormat;
        methodCount = builder.methodCount;
        methodOffset = builder.methodOffset;
        showThreadInfo = builder.showThreadInfo;
        logStrategy = builder.logStrategy;
        tag = builder.tag;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public void log(int priority, String onceOnlyTag, String message) {
        logNewLine(priority, tag);

        String tag = formatTag(onceOnlyTag);

        date.setTime(System.currentTimeMillis());

        StringBuilder builder = new StringBuilder();

        // machine-readable date/time
        builder.append(Long.toString(date.getTime()));

        // human-readable date/time
        builder.append(SEPARATOR);
        builder.append(dateFormat.format(date));

        // level
        builder.append(SEPARATOR);
        builder.append(Utils.logLevel(priority));

        // tag
        builder.append(SEPARATOR);
        builder.append(tag);

        builder.append(SEPARATOR);
        builder.append(message);

        logHeaderContent(priority, tag, methodCount);

        //get bytes of message with system's default charset (which is UTF-8 for Android)
        String msg = builder.toString();
        byte[] bytes = msg.getBytes();
        int length = bytes.length;
        if (length <= CHUNK_SIZE) {
            logContent(priority, tag, msg);
            return;
        } else {
            for (int i = 0; i < length; i += CHUNK_SIZE) {
                int count = Math.min(length - i, CHUNK_SIZE);
                //create a new String with system's default charset (which is UTF-8 for Android)
                logContent(priority, tag, new String(bytes, i, count));
            }
        }
        logNewLine(priority, tag);
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    private void logHeaderContent(int logType, String tag, int methodCount) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        if (showThreadInfo) {
            logChunk(logType, tag, " Thread: " + Thread.currentThread().getName());
        }
        String level = "";

        int stackOffset = getStackOffset(trace) + methodOffset;

        //corresponding method count with the current stack may exceeds the stack trace. Trims the count
        if (methodCount + stackOffset > trace.length) {
            methodCount = trace.length - stackOffset - 1;
        }

        for (int i = methodCount; i > 0; i--) {
            int stackIndex = i + stackOffset;
            if (stackIndex >= trace.length) {
                continue;
            }
            StringBuilder builder = new StringBuilder();
            builder
                    .append(" - ")
                    .append(level)
                    .append(getSimpleClassName(trace[stackIndex].getClassName()))
                    .append(".")
                    .append(trace[stackIndex].getMethodName())
                    .append(" ")
                    .append(" (")
                    .append(trace[stackIndex].getFileName())
                    .append(":")
                    .append(trace[stackIndex].getLineNumber())
                    .append(")");
            level += "   ";
            logChunk(logType, tag, builder.toString());
        }
        logNewLine(logType, tag);
    }

    private void logNewLine(int logType, String tag) {
        logChunk(logType, tag, NEW_LINE);
    }

    private void logContent(int logType, String tag, String chunk) {
        String[] lines = chunk.split(System.getProperty("line.separator"));
        for (String line : lines) {
            logChunk(logType, tag, " " + line + NEW_LINE);
        }
    }

    private void logChunk(int priority, String tag, String chunk) {
        logStrategy.log(priority, tag, chunk);
    }

    private String getSimpleClassName(String name) {
        int lastIndex = name.lastIndexOf(".");
        return name.substring(lastIndex + 1);
    }

    /**
     * Determines the starting index of the stack trace, after method calls made by this class.
     *
     * @param trace the stack trace
     * @return the stack offset
     */
    private int getStackOffset(StackTraceElement[] trace) {
        for (int i = MIN_STACK_OFFSET; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String name = e.getClassName();
            if (!name.equals(LoggerPrinter.class.getName()) && !name.equals(Logger.class.getName())) {
                return --i;
            }
        }
        return -1;
    }

    private String formatTag(String tag) {
        if (!Utils.isEmpty(tag) && !Utils.equals(this.tag, tag)) {
            return this.tag + "-" + tag;
        }
        return this.tag;
    }

    public static final class Builder {
        private static final int MAX_BYTES = 500 * 1024;

        Date date;
        SimpleDateFormat dateFormat;
        int methodCount = 1;
        int methodOffset = 0;
        boolean showThreadInfo = true;
        LogStrategy logStrategy;
        String tag = "PRETTY_LOGGER";

        private Builder() {
        }

        public Builder date(Date val) {
            date = val;
            return this;
        }

        public Builder dateFormat(SimpleDateFormat val) {
            dateFormat = val;
            return this;
        }

        public Builder methodCount(int val) {
            methodCount = val;
            return this;
        }

        public Builder methodOffset(int val) {
            methodOffset = val;
            return this;
        }

        public Builder showThreadInfo(boolean val) {
            showThreadInfo = val;
            return this;
        }

        public Builder logStrategy(LogStrategy val) {
            logStrategy = val;
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public TxtFormatStrategy build() {
            if (date == null) {
                date = new Date();
            }
            if (dateFormat == null) {
                dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS", Locale.CHINA);
            }
            if (logStrategy == null) {
                String diskPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
                Date d = new Date(System.currentTimeMillis());
                String folder = diskPath + File.separatorChar + "logger" + File.separatorChar + simpleDateFormat.format(d);

                HandlerThread ht = new HandlerThread("AndroidFileLogger." + folder);
                ht.start();
                Handler handler = new DiskTxtLogStrategy.WriteHandler(ht.getLooper(), folder, MAX_BYTES);
                logStrategy = new DiskTxtLogStrategy(handler);
            }
            return new TxtFormatStrategy(this);
        }
    }
}
