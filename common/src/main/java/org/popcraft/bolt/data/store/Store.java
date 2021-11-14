package org.popcraft.bolt.data.store;

import org.popcraft.bolt.data.Access;
import org.popcraft.bolt.data.protection.Protection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Store {
    List<Access> loadAccess();

    void saveAccess(Access access);

    Optional<Protection> loadProtection(UUID id);

    List<Protection> loadProtections();

    void saveProtection(Protection protection);
}
