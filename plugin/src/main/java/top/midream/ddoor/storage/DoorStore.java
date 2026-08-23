package top.midream.ddoor.storage;

import top.midream.ddoor.door.DoorRecord;

import java.util.List;

public interface DoorStore {

    void init() throws Exception;

    List<DoorRecord> loadAll() throws Exception;

    void upsert(DoorRecord door) throws Exception;

    void delete(java.util.UUID id) throws Exception;

    void close();
}
