package org.komamitsu.bank.commands;

import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "register-cert",
    description = "register-cert"
)
public class RegisterCert extends Base implements Callable<Void> {
    @Override
    public Void call() throws Exception {
        ClientServiceFactory factory = new ClientServiceFactory();
        try (ClientService service = factory.create(new ClientConfig(base.path.toFile()))) {
            service.registerCertificate();
        }
        return null;
    }
}
