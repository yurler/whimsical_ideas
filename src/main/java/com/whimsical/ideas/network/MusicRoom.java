package com.whimsical.ideas.network;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MusicRoom {
    public String roomName;
    public UUID hostUUID;
    public String hostName;
    public String roomType; // PUBLIC, PRIVATE, HIDDEN
    public String password;
    public Set<UUID> invitedPlayers = new HashSet<>();
    public Set<UUID> listeners = new HashSet<>();

    public MusicRoom(String roomName, UUID hostUUID, String hostName, String roomType, String password) {
        this.roomName = roomName;
        this.hostUUID = hostUUID;
        this.hostName = hostName;
        this.roomType = roomType;
        this.password = password;
    }
}