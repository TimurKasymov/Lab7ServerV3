package src.db.DI;

import java.sql.SQLException;
import java.util.List;

public interface DbCollectionManager<TEnt> {
    boolean insert(TEnt entity);
    boolean update(TEnt entity);
    boolean delete(TEnt id);
    List<TEnt> load();
    boolean isThisLastServerToTouchDB(int port);
    void markThatThisServerHasMadeChangesToDb();
    void markReversedCollection();
    Integer getNextId(String seqName);
}
