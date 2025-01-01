package com.butterfly.sdk.utils

import java.lang.Exception
import java.lang.ref.WeakReference

class EventBus {
    abstract class Event

    interface Listener {
        fun onEvent(event: Event)
    }

    companion object {
        private val listeners: HashMap<Class<out Event>, MutableSet<ListenerToken>> = HashMap()
        fun notify(event: Event) {
            val clone = listeners[event::class.java]?.toList() ?: return
            clone.forEach {
                try {
                    it.listener?.onEvent(event) ?: run {
                        listeners[event::class.java]?.let { registeredListeners ->
                            synchronized(registeredListeners) {
                                registeredListeners.remove(it)// { it.listener == listener }
                            }
                        }
                    }
                } catch (e: Throwable) {
                    //
                }
            }
        }

        private fun synchronouslyRemove(listener: Listener, registeredListeners: MutableSet<ListenerToken>) {
            if (!registeredListeners.any { it.listener == listener }) return

            synchronized(registeredListeners) {
                registeredListeners.removeAll { it.listener == listener }
            }
        }

        fun remove(listener: Listener, event: Class<out Event>?) {

            event?.let {
                val registeredListeners = listeners[event] ?: return
                synchronouslyRemove(listener, registeredListeners)
            } ?: run {
                listeners.values.forEach {
                    synchronouslyRemove(listener, it)
                }
            }
        }

        fun addListener(listener: Listener, vararg events: Class<out Event>): ListenerToken? {
            val token = ListenerToken(listener)

            events.forEach {
                if (listeners[it] == null) {
                    listeners[it] = mutableSetOf()
                }

                val registeredListeners = listeners[it] ?: return null

                synchronized(registeredListeners) {
                    registeredListeners.add(token)
                }
            }

            return token
        }
    }

    class ListenerToken(listener: Listener) {
        fun remove() {
            this.listener?.let {
                remove(it, null)
            }
        }

        private val ref: WeakReference<Listener> = WeakReference(listener)
        val listener: Listener? get() = try {
            ref.get()
        } catch (e: Exception) {
            null
        }
    }
}