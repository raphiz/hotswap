package io.github.raphiz.zackbum

import org.slf4j.LoggerFactory

private val stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)

internal fun logger() = LoggerFactory.getLogger(stackWalker.callerClass)
