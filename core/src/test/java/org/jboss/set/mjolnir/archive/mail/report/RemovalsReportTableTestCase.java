package org.jboss.set.mjolnir.archive.mail.report;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.jboss.set.mjolnir.archive.domain.RemovalStatus;
import org.jboss.set.mjolnir.archive.domain.UserRemoval;
import org.jboss.set.mjolnir.archive.mail.RemovalsReportBean;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.set.mjolnir.archive.util.TestUtils.createUserRemoval;

@RunWith(CdiTestRunner.class)
public class RemovalsReportTableTestCase {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Inject
    private EntityManager em;

    @Inject
    private RemovalsReportBean removalsReportBean;

    @Inject
    private RemovalsReportTable removalsReportTable;

    @Before
    public void setup() throws IllegalAccessException, NoSuchFieldException {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_WEEK, 1);

        Timestamp oneDayLater = new Timestamp(calendar.getTime().getTime());

        em.getTransaction().begin();

        UserRemoval userRemoval = createUserRemoval("User1", oneDayLater, oneDayLater, RemovalStatus.COMPLETED);
        em.persist(userRemoval);

        userRemoval = createUserRemoval("User2", oneDayLater, oneDayLater, RemovalStatus.FAILED);
        em.persist(userRemoval);

        em.getTransaction().commit();
    }

    @Test
    public void testComposeTableBody() {
        List<UserRemoval> lastFinishedRemovals = removalsReportBean.getLastFinishedRemovals();

        String messageBody = removalsReportTable.composeTable();
        Document doc = Jsoup.parse(messageBody);

        assertThat(doc.select("tr").size()).isEqualTo(lastFinishedRemovals.size() + 1);
        assertThat(doc.select("th").text()).isEqualTo("Name Created Started Status");

        Elements elements = doc.select("td");
        assertThat(elements.size()).isEqualTo(lastFinishedRemovals.size() * 4);

        for (int i = 0; i < lastFinishedRemovals.size(); i++) {
            assertThat(elements.get((i * 4)).childNode(0).toString()).isEqualTo(lastFinishedRemovals.get(i).getUsername());
            assertThat(elements.get((i * 4) + 1).childNode(0).toString()).isEqualTo(lastFinishedRemovals.get(i).getCreated().toString());
            assertThat(elements.get((i * 4) + 2).childNode(0).toString()).isEqualTo(lastFinishedRemovals.get(i).getStarted().toString());
            assertThat(elements.get((i * 4) + 3).childNode(0).toString()).isEqualTo(lastFinishedRemovals.get(i).getStatus().toString());
        }
    }
}