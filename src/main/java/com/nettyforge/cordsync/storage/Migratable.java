package com.nettyforge.cordsync.storage;

import java.util.Map;
import java.util.UUID;


public interface Migratable {



    Map<UUID, LinkedData> loadAllLinkedAccounts();


    void importLinkedAccounts(Map<UUID, LinkedData> accounts);


    class LinkedData {
        public final String playerName;
        public final String discordId;

        public LinkedData(String playerName, String discordId) {
            this.playerName = playerName;
            this.discordId = discordId;
        }
    }
}


