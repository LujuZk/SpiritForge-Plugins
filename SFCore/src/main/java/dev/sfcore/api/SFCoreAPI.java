package dev.sfcore.api;

import dev.sfcore.database.AsyncDatabaseExecutor;
import dev.sfcore.database.SFDatabase;
import dev.sfcore.database.SFDatabaseFactory;
import dev.sfcore.database.SqlDialect;
import dev.sfcore.managers.StatManager;
import dev.sfcore.managers.ManaManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SFCoreAPI {

    private static SFCoreAPI instance;
    private final StatManager manager;
    private final ManaManager manaManager;
    private final SFDatabaseFactory databaseFactory;
    private final AsyncDatabaseExecutor asyncExecutor;

    private SFCoreAPI(StatManager manager, ManaManager manaManager, SFDatabaseFactory databaseFactory,
                      AsyncDatabaseExecutor asyncExecutor) {
        this.manager = manager;
        this.manaManager = manaManager;
        this.databaseFactory = databaseFactory;
        this.asyncExecutor = asyncExecutor;
    }

    public static SFCoreAPI get() {
        if (instance == null) throw new IllegalStateException("SFCore is not loaded");
        return instance;
    }


    public static void init(StatManager manager, manaManager, SFDatabaseFactory databaseFactory,
                            AsyncDatabaseExecutor asyncExecutor) {
        instance = new SFCoreAPI(manager, manaManager, databaseFactory, asyncExecutor);
    }

    public static void shutdown() {
        instance = null;
    }

    // ─── Database API ────────────────────────────────────────────────

    /**
     * Devuelve el handle de base de datos del plugin consumidor.
     * El motor (SQLite/MySQL) y las rutas/prefijos de tabla los resuelve SFCore
     * a partir de su config.yml global. El consumidor solo debe usar
     * {@code db.getConnection()} en try-with-resources y {@code db.getTableName("...")}
     * para resolver nombres de tabla lógicos.
     */
    public SFDatabase getDatabase(String namespace) {
        return databaseFactory.get(namespace);
    }

    /** Dialecto SQL activo (upserts, tipos, etc.) — compartido entre namespaces. */
    public SqlDialect getDialect() {
        return databaseFactory.dialect();
    }

    /** Executor async compartido para operaciones de DB no bloqueantes. */
    public AsyncDatabaseExecutor getAsyncExecutor() {
        return asyncExecutor;
    }

    public void addBonus(Player player, String source, StatType stat, double value) {
        manager.addBonus(player, source, stat, value);
    }

    public void removeBonus(Player player, String source) {
        manager.removeBonus(player, source);
    }

    public void clearSource(Player player, String sourcePrefix) {
        manager.clearSource(player, sourcePrefix);
    }

    public double getTotal(Player player, StatType stat) {
        return manager.getTotal(player, stat);
    }

    public double getMagicDamage(Player player) {
        return manager.getTotal(player, StatType.MAGIC_DAMAGE);
    }

    public Map<StatType, Double> getAllTotals(Player player) {
        return manager.getAllTotals(player);
    }

    public double getMana(Player player) {
        return manaManager == null ? 0.0D : manaManager.getMana(player);
    }

    public double getMaxMana(Player player) {
        return manaManager == null ? 0.0D : manaManager.getMaxMana(player);
    }

    public double getManaRegenPerSecond(Player player) {
        return manaManager == null ? 0.0D : manaManager.getManaRegenPerSecond(player);
    }

    public void setMana(Player player, double value) {
        if (manaManager != null) {
            manaManager.setMana(player, value);
        }
    }

    public void addMana(Player player, double value) {
        if (manaManager != null) {
            manaManager.addMana(player, value);
        }
    }

    public boolean spendMana(Player player, double value) {
        return manaManager == null || manaManager.spendMana(player, value);
    }

    // ─── Integration Helpers ─────────────────────────────────────────

    public static boolean isAvailable() {
        return instance != null;
    }

    public static boolean isValidStatKey(String key) {
        return StatType.fromKey(key) != null;
    }

    public static Optional<StatType> findStatType(String key) {
        return Optional.ofNullable(StatType.fromKey(key));
    }

    /**
     * Agrega múltiples bonuses en bulk con un solo reapply al final.
     * Más eficiente que llamar addBonus() N veces.
     */
    public void addBonuses(Player player, List<StatBonus> bonuses) {
        manager.addBonusesBulk(player, bonuses);
    }
}
