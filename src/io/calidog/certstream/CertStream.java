package io.calidog.certstream;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.CertificateException;
import java.util.function.Consumer;

/**
 * The main class for handling {@link CertStreamMessage}s.
 */
public class CertStream{

    private static final Logger logger = LoggerFactory.getLogger(CertStream.class);

    private static BoringParts theBoringParts = new BoringParts();

    private static CertStreamClientImplFactory defaultImplFactory =
            new CertStreamClientImplFactory(theBoringParts,
                    theBoringParts,
                    theBoringParts);

    /**
     * @param handler A {@link Consumer<String>} that we'll
     *                run in a Thread that stays alive as long
     *                as the WebSocket stays open.
     */
    public static void onMessageString(Consumer<String> handler)
    {
        new Thread(() ->
        {
            defaultImplFactory.make(handler::accept);

            while (theBoringParts.isNotClosed())
            {
                Thread.yield();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }


    /**
     * @param handler A {@link Consumer<CertStreamMessage>} that we'll
     *                run in a Thread that stays alive as long
     *                as the WebSocket stays open.
     */
    public static void onMessage(CertStreamMessageHandler handler)
    {
        onMessageString(string ->
        {
            CertStreamMessagePOJO msg;
            try
            {
                msg = new Gson().fromJson(string, CertStreamMessagePOJO.class);

                if (msg.messageType.equalsIgnoreCase("heartbeat"))
                {
                    return;
                }
            }catch (JsonSyntaxException e)
            {
                System.out.println(e.getMessage());
                logger.warn("onMessage had an exception parsing some json", e);
                return;
            }

            CertStreamMessage fullMsg;

            try
            {
                fullMsg = CertStreamMessage.fromPOJO(msg);
            } catch (CertificateException e) {
                logger.warn("Encountered a CertificateException", e);
                return;
            }

            handler.onMessage(fullMsg);
        });
    }
}
