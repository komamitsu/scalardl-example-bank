package org.komamitsu.bank;

import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.schemaloader.SchemaLoader;
import com.scalar.db.schemaloader.SchemaLoaderException;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntTestMain {
    private String configPath;
    private String schemaConfigPath;

    @BeforeEach
    void setUp() throws SchemaLoaderException {
        configPath = System.getenv("SCALARDB_EXAMPLE_IT_CONFIG_PATH");
        Assumptions.assumeTrue(configPath != null);
        schemaConfigPath = System.getenv("SCALARDB_EXAMPLE_IT_SCHEMA_CONFIG_PATH");
        Assumptions.assumeTrue(schemaConfigPath != null);

        SchemaLoader.unload(Paths.get(configPath), Paths.get(schemaConfigPath), true);
        SchemaLoader.load(Paths.get(configPath), Paths.get(schemaConfigPath), Collections.emptyMap(), true);
    }

    private void withStdoutCapture(Runnable proc, Consumer<String> validator) throws Exception {
        PrintStream origStdout = System.out;

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); PrintStream ps = new PrintStream(bos)) {
            System.setOut(ps);
            proc.run();
            ps.flush();
            System.setOut(origStdout);
            if (validator != null) {
                validator.accept(bos.toString());
            }
        }
        finally {
            System.setOut(origStdout);
        }
    }

    private void runCli(String[] args) {
        try {
            Main.main(args);
        } catch (IOException | TransactionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void showButNotFound() throws Exception {
        withStdoutCapture(
                () -> runCli(new String[] {"--config", configPath, "show", "--id", "alice"}),
                (result) -> Assertions.assertEquals("Optional.empty", result.strip())
        );
    }

    @Test
    void create() throws Exception {
        runCli(new String[] {"--config", configPath, "create", "--id", "alice", "--hp", "100", "--attack", "15"});
        withStdoutCapture(
                () -> runCli(new String[] {"--config", configPath, "show", "--id", "alice"}),
                (result) -> Assertions.assertEquals("Optional[Player[id=alice, hp=100, attack=15]]", result.strip())
        );
    }

    @Test
    void delete() throws Exception {
        runCli(new String[] {"--config", configPath, "create", "--id", "alice", "--hp", "100", "--attack", "15"});
        runCli(new String[] {"--config", configPath, "delete", "--id", "alice"});
        withStdoutCapture(
                () -> runCli(new String[] {"--config", configPath, "show", "--id", "alice"}),
                (result) -> Assertions.assertEquals("Optional.empty", result.strip())
        );
    }

    @Test
    void attack() throws Exception {
        runCli(new String[] {"--config", configPath, "create", "--id", "alice", "--hp", "100", "--attack", "15"});
        runCli(new String[] {"--config", configPath, "create", "--id", "bob", "--hp", "200", "--attack", "8"});
        runCli(new String[] {"--config", configPath, "attack", "--id", "alice", "--other-id", "bob"});
        withStdoutCapture(
                () -> runCli(new String[] {"--config", configPath, "show", "--id", "bob"}),
                (result) -> Assertions.assertEquals("Optional[Player[id=bob, hp=185, attack=8]]", result.strip())
        );
    }

    @Test
    void obtainBonus() throws Exception {
        runCli(new String[] {"--config", configPath, "create", "--id", "alice", "--hp", "100", "--attack", "15"});
        runCli(new String[] {"--config", configPath, "create", "--id", "bob", "--hp", "200", "--attack", "8"});
        runCli(new String[] {"--config", configPath, "bonus", "--id", "alice", "--other-id", "bob", "--threshold", "300", "--bonus", "100"});
        withStdoutCapture(
                () -> runCli(new String[] {"--config", configPath, "show", "--id", "alice"}),
                (result) -> Assertions.assertEquals("Optional[Player[id=alice, hp=200, attack=15]]", result.strip())
        );
    }

    @Test
    void obtainNoBonus() throws Exception {
        runCli(new String[] {"--config", configPath, "create", "--id", "alice", "--hp", "100", "--attack", "15"});
        runCli(new String[] {"--config", configPath, "create", "--id", "bob", "--hp", "200", "--attack", "8"});
        runCli(new String[] {"--config", configPath, "bonus", "--id", "alice", "--other-id", "bob", "--threshold", "290", "--bonus", "100"});
        withStdoutCapture(
                () -> runCli(new String[] {"--config", configPath, "show", "--id", "alice"}),
                (result) -> Assertions.assertEquals("Optional[Player[id=alice, hp=100, attack=15]]", result.strip())
        );
    }

    private record Pair<F, S>(F first, S second) {}

    private PlayerServiceException findPlayerServiceException(Throwable origEx) {
        Throwable ex = origEx;
        while (true) {
            if (ex == null) {
                return null;
            }
            if (ex instanceof PlayerServiceException playerServiceException) {
                return playerServiceException;
            }
            ex = ex.getCause();
        }
    }

    @Test
    void runBonusConcurrently() throws Exception {
        ExecutorService executorService = Executors.newCachedThreadPool();
        Pattern pattern = Pattern.compile("Optional\\[Player\\[id=\\w+, hp=(?<hp>\\d+), attack=\\d+\\]\\]");

        Map<Pair<Class<? extends Throwable>, String>, Integer> errorTable = new HashMap<>();

        Random random = new Random();
        for (int i = 0; i < 40; i++) {
            String aliceId = "alice" + i;
            String bobId = "bob" + i;

            runCli(new String[] {"--config", configPath, "delete", "--id", aliceId});
            runCli(new String[]{"--config", configPath, "create", "--id", aliceId, "--hp", "100", "--attack", "15"});

            runCli(new String[] {"--config", configPath, "delete", "--id", bobId});
            runCli(new String[]{"--config", configPath, "create", "--id", bobId, "--hp", "200", "--attack", "8"});

            Future<?> futureForAlice = executorService.submit(() ->
                    runCli(new String[]{"--config", configPath, "bonus", "--id", aliceId, "--other-id", bobId, "--threshold", "300", "--bonus", "100"}));

            // This might be a freaky test, but most of the cases can result in conflicts without this wait...
            TimeUnit.MILLISECONDS.sleep(random.nextInt(100));

            Future<?> futureForBob = executorService.submit(() ->
                    runCli(new String[]{"--config", configPath, "bonus", "--id", bobId, "--other-id", aliceId, "--threshold", "300", "--bonus", "100"}));
            try {
                futureForAlice.get(60, TimeUnit.SECONDS);
                futureForBob.get(60, TimeUnit.SECONDS);
            }
            catch (Throwable e) {
                PlayerServiceException playerServiceEx = findPlayerServiceException(e);

                if (playerServiceEx != null) {
                    Throwable cause = playerServiceEx.getCause();
                    Pair<Class<? extends Throwable>, String> errorKey = new Pair<>(cause.getClass(), cause.getMessage());
                    Integer v = errorTable.computeIfAbsent(errorKey, k -> 0);
                    errorTable.put(errorKey, v + 1);
                    continue;
                }
                throw e;
            }

            AtomicReference<Integer> hpOfAlice = new AtomicReference<>();
            withStdoutCapture(
                    () -> runCli(new String[] {"--config", configPath, "show", "--id", aliceId}),
                    (result) -> {
                        Matcher matcher = pattern.matcher(result.strip());
                        assertTrue(matcher.find());
                        hpOfAlice.set(Integer.parseInt(matcher.group("hp")));
                    }
            );
            AtomicReference<Integer> hpOfBob = new AtomicReference<>();
            withStdoutCapture(
                    () -> runCli(new String[] {"--config", configPath, "show", "--id", bobId}),
                    (result) -> {
                        Matcher matcher = pattern.matcher(result.strip());
                        assertTrue(matcher.find());
                        hpOfBob.set(Integer.parseInt(matcher.group("hp")));
                    }
            );
            assertEquals(100 + 200 + 100, hpOfAlice.get() + hpOfBob.get());
        }

        System.out.println("errorTable: " + errorTable);
    }
}
