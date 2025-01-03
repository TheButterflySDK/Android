package com.butterfly.sdk.utils

import java.lang.Exception
import java.lang.ref.WeakReference

class EventBus {
    abstract class Event

    interface Listener {
        fun onEvent(event: Event)
    }

    private val listeners: HashMap<Class<out Event>, HashMap<String, ListenerToken>> = HashMap()

    fun notify(event: Event) {
        val clone = listeners[event::class.java]?.entries?.toList() ?: return
        clone.forEach {
            try {
                it.value.listener?.onEvent(event) ?: run {
                    listeners[event::class.java]?.let { registeredListeners ->
                        synchronized(registeredListeners) {
                            registeredListeners.remove(it.key)// { it.listener == listener }
                        }
                    }
                }
            } catch (e: Throwable) {
                //
            }
        }
    }

    private fun synchronouslyRemove(listener: Listener, registeredListeners: HashMap<String, ListenerToken>) {
        val found = registeredListeners.filter { it.value.listener == listener }
        if (found.isEmpty()) return

        synchronized(registeredListeners) {
            found.forEach { entry ->
                registeredListeners.remove(entry.key)
            }
        }
    }

    fun remove(token: ListenerToken): Boolean {
        var removed = 0
        token.events.forEach { eventClass ->
            val registeredListeners = listeners[eventClass] ?: return false
            synchronized(registeredListeners) {
                if (token == registeredListeners.remove(token.id.toString())) {
                    removed++
                }
            }
        }

        return removed == token.events.size
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

    fun addListener(listener: Listener, vararg events: Class<out Event>): ListenerToken {
        val token = ListenerToken(listener, this, events.toList())

        events.forEach {
            if (listeners[it] == null) {
                listeners[it] = HashMap()
            }

            listeners[it]?.let { registeredListeners: HashMap<String, ListenerToken> ->
                synchronized(registeredListeners) {
                    registeredListeners[token.id.toString()] = token
                }
            }
        }

        return token
    }

    class ListenerToken(listener: Listener, private val eventBus: EventBus, val events: List<Class<out Event>>) {
        companion object {
            var counter = 0
        }

        val id: Int = counter++
        private val ref: WeakReference<Listener>
        val listener: Listener? get() = try {
            ref.get()
        } catch (e: Exception) {
            null
        }

        init {
            ref = WeakReference(listener)
        }

        fun remove(): Boolean {
            return eventBus.remove(this)
        }
    }
}