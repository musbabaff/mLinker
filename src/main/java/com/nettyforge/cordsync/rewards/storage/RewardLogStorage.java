package com.nettyforge.cordsync.rewards.storage;

import java.util.UUID;


public interface RewardLogStorage {


    void log(UUID player, String rewardType, String details);


    void close();
}


