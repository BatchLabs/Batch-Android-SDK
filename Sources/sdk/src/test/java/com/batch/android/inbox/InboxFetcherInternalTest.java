package com.batch.android.inbox;

import static android.os.Looper.getMainLooper;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.annotation.LooperMode.Mode.PAUSED;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.BatchInboxFetcher;
import com.batch.android.BatchInboxNotificationContent;
import com.batch.android.di.providers.InboxFetcherInternalProvider;
import com.batch.android.json.JSONObject;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.annotation.LooperMode;

@RunWith(AndroidJUnit4.class)
@LooperMode(PAUSED)
@PowerMockIgnore({ "org.robolectric.*", "android.*", "androidx.*" })
@SmallTest
@PrepareForTest({ InboxFetcherInternal.class })
public class InboxFetcherInternalTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private Context appContext;
    private String payload =
        "{\"notifications\":[{\"notificationId\":\"163038c3-987a-11ea-a126-c7d4eb63a32d\",\"notificationTime\":1585387968076,\"sendId\":\"7256892-e5c3-4b22-86a2-88e8f556740a\",\"payload\":{\"com.batch\":{\"t\":\"c\",\"l\":\"https://batch.com/jouets/jeux-creatifs.html\",\"i\":\"76497434-e5c3-4b32-86a2-68e8f556740a\",\"od\":{\"n\":\"163038c3-70d7-11ea-a126-c7d4eb63a32d\",\"ct\":\"654987654246ff48c9e74bfde50769c9e\"}},\"date_expiration\":\"2020-04-03\",\"message\":\"Dessiner, peindre, modeler… laissez la créativité de vos enfants s’exprimer !\",\"msg\":\"Dessiner, peindre, modeler… laissez la créativité de vos enfants s’exprimer !\",\"title\":\"Découvrez nos jeux créatifs\"}},{\"notificationId\":\"36f52be6-6584-11ea-6548-679b395af53e\",\"notificationTime\":1584868332011,\"sendId\":\"615815052518-23bf-43de-a509-5ab65b146667\",\"payload\":{\"com.batch\":{\"t\":\"c\",\"l\":\"https://batch.com/jardin-plein-air/amenagement-jardin/outils-jardinage.html\",\"i\":\"ff9dc099-265654-43de-a509-5ab65b146667\",\"od\":{\"n\":\"36f52be6-6c1d-0000-a186-679b395af53e\",\"ct\":\"d361add5a5a6a8a2941ffa8c231e\"}},\"date_expiration\":\"2020-03-29\",\"message\":\"Arrosoirs, râteaux, pelles…\",\"msg\":\"Arrosoirs, râteaux, pelles…\",\"title\":\"Outils de jardinage à petits prix !\"}},{\"notificationId\":\"2db21045-6ac4-0000-0000-c7d4eb63a32d\",\"notificationTime\":1584720140100,\"sendId\":\"0b38775a-0000-0000-b11c-fd8bc69648f1\",\"payload\":{\"com.batch\":{\"t\":\"c\",\"l\":\"https://batch.fcom/entretien-rangement/entretien.html\",\"i\":\"0b38775a-0000-0000-b11c-fd8bc69648f1\",\"od\":{\"n\":\"2db21045-6ac4-0000-a126-c7d4eb63a32d\",\"ct\":\"651294080ac6aad19866259fb4335cd5\"}},\"date_expiration\":\"2020-03-27\",\"message\":\"Découvrez toutes nos idées de Génie pour que votre intérieur respire la fraicheur !\",\"msg\":\"Découvrez toutes nos idées de Génie pour que votre intérieur respire la fraicheur !\",\"title\":\"Tout pour nettoyer votre intérieur\"}},{\"notificationId\":\"4ecd6018-69b9-11ea-0000-7b8ab50e1203\",\"notificationTime\":1584605520017,\"sendId\":\"70064811-7052-0000-bed0-c7f683769b3a\",\"payload\":{\"com.batch\":{\"t\":\"c\",\"l\":\"https://batch.com/543499-patch-electrostimulation-musculaire-abdos-et-bras-sans-fil.html\",\"i\":\"70064811-0000-420b-0000-c7f683769b3a\",\"od\":{\"n\":\"4ecd6018-0000-11ea-0000-7b8ab50e1203\",\"ct\":\"f602fa3356000000075b45d4597841\"}},\"date_expiration\":\"2020-03-26\",\"message\":\"Tonifiez vos abdos et vos bras avec l’électrostimulation pour seulement 14.90€ !\",\"msg\":\"Tonifiez vos abdos et vos bras avec l’électrostimulation pour seulement 14.90€ !\",\"title\":\"Le sport à la maison n’a jamais été aussi simple\"}},{\"notificationId\":\"4b33ae59-6872-10101-0000-c5c3b5ad2209\",\"notificationTime\":1584465068546,\"sendId\":\"bac0161f-b1d4-0000-0000-7a04ea98c5c5\",\"payload\":{\"com.batch\":{\"t\":\"c\",\"l\":\"https://batch.com/\",\"i\":\"bac0161f-b1d4-0000-0000-7a04ea98c5c5\",\"od\":{\"n\":\"4b33ae59-6872-0000-0000-c5c3b5ad2209\",\"ct\":\"57925a9240000098dd81697adf24257\"}},\"date_expiration\":\"2020-03-24\",\"message\":\"Nous ferons le maximum pour honorer votre commande dans les meilleurs délais. \",\"msg\":\"Nous ferons le maximum pour honorer votre commande dans les meilleurs délais. \",\"title\":\"Faites-vous livrer !\"}},{\"notificationId\":\"df2ce0a8-0000-11ea-0000-c5c3b5ad2209\",\"notificationTime\":1584190868394,\"sendId\":\"306f5168-0000-0000-bb18-eaed0b03233a\",\"payload\":{\"com.batch\":{\"t\":\"c\",\"l\":\"https://batch.com/516482-coffre-320-l-noir.html\",\"i\":\"306f5168-0000-0000-bb18-eaed0b03233a\",\"od\":{\"n\":\"df2ce0a8-65f3-0000-0000-c5c3b5ad2209\",\"ct\":\"4d1a71e00000000ba4fa7444e6e98139\"},\"bi\":{\"u\":\"https://m.batch.com/push/icons/1YfJvJVM2/c3bef144f547cd73c0000000182fe0c5a2d98c095de8f9c4802edc.jpg\",\"d\":[1,2,3,1.5]}},\"date_expiration\":\"2020-03-21\",\"message\":\"Le coffre de jardin XL 320L noir imperméable et cadenassable à 25€. Grande capacité pour tout protéger : outils, jouets, coussins…\",\"msg\":\"Le coffre de jardin XL 320L noir imperméable et cadenassable à 25€. Grande capacité pour tout protéger : outils, jouets, coussins…\",\"title\":\"Aménagez votre jardin !\"}}],\"hasMore\":true,\"timeout\":false,\"cursor\":\"f7a441ab-4f4b-0000-0000-09dc36ea4510\"}";
    private CountDownLatch latch;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
        latch = new CountDownLatch(1);
    }

    @Test
    public void testFetchNewNotifications() throws Exception {
        PowerMockito
            .whenNew(InboxFetchWebserviceClient.class)
            .withAnyArguments()
            .then(
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        InboxFetchWebserviceClient client = new InboxFetchWebserviceClient(
                            invocation.getArgument(0),
                            invocation.getArgument(1),
                            invocation.getArgument(2),
                            invocation.getArgument(3),
                            invocation.getArgument(4),
                            invocation.getArgument(5),
                            invocation.getArgument(6),
                            invocation.getArgument(7)
                        );
                        InboxFetchWebserviceClient spy = PowerMockito.spy(client);
                        PowerMockito.doReturn(new JSONObject(payload)).when(spy, "getBasicJsonResponseBody");
                        return spy;
                    }
                }
            );

        InboxFetcherInternal fetcher = InboxFetcherInternalProvider.get(appContext, "test-install-id");
        fetcher.fetchNewNotifications(
            new BatchInboxFetcher.OnNewNotificationsFetchedListener() {
                @Override
                public void onFetchSuccess(
                    @NonNull List<BatchInboxNotificationContent> notifications,
                    boolean foundNewNotifications,
                    boolean endReached
                ) {
                    Assert.assertTrue(foundNewNotifications);
                    Assert.assertFalse(endReached);
                    Assert.assertEquals(6, notifications.size());
                    latch.countDown();
                }

                @Override
                public void onFetchFailure(@NonNull String error) {
                    Assert.fail();
                    latch.countDown();
                }
            }
        );
        // execute all tasks posted to main looper
        shadowOf(getMainLooper()).idle();
        latch.await();

        // Test markAsRead
        List<BatchInboxNotificationContent> notifications = fetcher.getPublicFetchedNotifications();
        Assert.assertEquals(6, notifications.size());

        BatchInboxNotificationContent notification = notifications.get(0);
        Assert.assertTrue(notification.isUnread());
        fetcher.markAsRead(notification);
        Assert.assertFalse(notification.isUnread());

        notifications = fetcher.getPublicFetchedNotifications();
        notification = notifications.get(0);
        Assert.assertFalse(notification.isUnread());

        // Test markAllAsRead
        fetcher.markAllAsRead();
        notifications = fetcher.getPublicFetchedNotifications();
        Assert.assertEquals(6, notifications.size());
        Assert.assertFalse(notifications.get(0).isUnread());
        Assert.assertFalse(notifications.get(1).isUnread());
        Assert.assertFalse(notifications.get(2).isUnread());
        Assert.assertFalse(notifications.get(3).isUnread());
        Assert.assertFalse(notifications.get(4).isUnread());
        Assert.assertFalse(notifications.get(5).isUnread());

        // Test markAsDelete
        notifications = fetcher.getPublicFetchedNotifications();
        notification = notifications.get(1);

        Assert.assertFalse(notification.isDeleted());
        fetcher.markAsDeleted(notification);
        Assert.assertTrue(notification.isDeleted());

        notifications = fetcher.getPublicFetchedNotifications();
        Assert.assertEquals(5, notifications.size());
        BatchInboxNotificationContent compareNotification = notifications.get(1);
        Assert.assertNotEquals(
            notification.getNotificationIdentifier(),
            compareNotification.getNotificationIdentifier()
        );
    }
}
