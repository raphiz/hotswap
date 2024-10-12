package io.github.raphiz.zackbum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LoggerHelpers {
    private static final StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    static Logger logger() {
        String name = stackWalker.getCallerClass().getName();
        return LoggerFactory.getLogger(name);
    }
}
