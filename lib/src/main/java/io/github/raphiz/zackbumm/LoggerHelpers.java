package io.github.raphiz.zackbumm;

import java.util.logging.Logger;

class LoggerHelpers {
    private static final StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    static Logger logger() {
        String name = stackWalker.getCallerClass().getName();
        return Logger.getLogger(name);
    }
}
