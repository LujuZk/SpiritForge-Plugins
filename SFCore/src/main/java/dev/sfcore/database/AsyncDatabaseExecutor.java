package dev.sfcore.database;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executor dedicado para operaciones de base de datos asincronas.
 * Provee fire-and-forget para writes, CompletableFuture para reads,
 * y callback en main thread para aplicar resultados a Bukkit API.
 */
public final class AsyncDatabaseExecutor {

    private static final Logger log = Logger.getLogger("SFCore");
    private final ExecutorService executor;
    private final Plugin plugin;

    public AsyncDatabaseExecutor(Plugin plugin, int threadCount) {
        this.plugin = plugin;
        this.executor = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r, "SFCore-DB-Async");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Fire-and-forget: ejecuta un write en el pool async.
     * Loguea errores pero no los propaga.
     */
    public void runAsync(Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.log(Level.WARNING, "[SFCore-Async] Error in async DB write", e);
            }
        });
    }

    /**
     * Ejecuta un read async y devuelve un CompletableFuture con el resultado.
     */
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }

    /**
     * Encadena un callback en el main thread de Bukkit sobre un CompletableFuture.
     * Si el future falla, loguea el error y no ejecuta el callback.
     */
    public <T> void thenOnMain(CompletableFuture<T> future, Consumer<T> callback) {
        future.thenAccept(result -> {
            if (Bukkit.isPrimaryThread()) {
                callback.accept(result);
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
            }
        }).exceptionally(ex -> {
            log.log(Level.WARNING, "[SFCore-Async] Error in async DB operation", ex);
            return null;
        });
    }

    /**
     * Apaga el executor y espera a que las tareas pendientes terminen.
     * Llamar desde SFCorePlugin.onDisable() ANTES de cerrar los pools de HikariCP.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warning("[SFCore-Async] Timeout esperando tareas pendientes. Forzando shutdown.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
