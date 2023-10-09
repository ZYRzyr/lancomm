package com.fighter.lancomm

import com.fighter.lancomm.broadcast.Broadcaster
import com.fighter.lancomm.ptop.Command
import com.fighter.lancomm.ptop.Communicator
import com.fighter.lancomm.receive.Receiver
import com.fighter.lancomm.search.Searcher

val broadcaster: Broadcaster = LanCommManager.getBroadcaster()

val receiver: Receiver = LanCommManager.getReceiver()

val searcher: Searcher = LanCommManager.getSearcher()

val communicator: Communicator = LanCommManager.getCommunicator()

fun sendCommand(block: Command.() -> Unit) {
    val command = Command()
    block(command)
    communicator.sendCommand(command)
}
