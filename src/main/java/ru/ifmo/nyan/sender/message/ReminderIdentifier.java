package ru.ifmo.nyan.sender.message;

public class ReminderIdentifier {
    final long factoryId;
    final long reminderId;

    /**
     * Can be instantiated only by ReminderProtocol
     */
    ReminderIdentifier(long factoryId, long reminderId) {
        this.factoryId = factoryId;
        this.reminderId = reminderId;
    }

}
