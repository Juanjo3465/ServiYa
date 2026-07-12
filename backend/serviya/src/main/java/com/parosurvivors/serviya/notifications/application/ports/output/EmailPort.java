package com.parosurvivors.serviya.notifications.application.ports.output;

import java.util.Map;

public interface EmailPort {

    boolean send(Long userId, String type, String title, String message, Map<String, String> protectedData);
}
