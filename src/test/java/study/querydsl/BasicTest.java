package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Transactional
@SpringBootTest
class BasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory query;

    @BeforeEach
    void contextLoads() {
        query = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 100, teamA);
        Member member2 = new Member("member2", 100, teamA);

        Member member3 = new Member("member3", 100, teamB);
        Member member4 = new Member("member4", 100, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();

        List<Member> members = em.createQuery("select m from Member m", Member.class)
                .getResultList();

        for (Member member : members) {
            System.out.println("member = " + member);
            System.out.println("-> member.team " + member.getTeam());
        }
    }

    @Test
    public void startJPQL() {

        Member member = em.createQuery("select m from Member m WHERE m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(member.getUsername()).isEqualTo("member1");

    }


    @Test
    public void dsl() {

        Member m = query
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(m.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {

        Member findMember = query.selectFrom(QMember.member)
                .where(
                        QMember.member.username.eq("member1"),
                        QMember.member.age.eq(1)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {

        List<Member> members = query.selectFrom(member)
                .fetch();

        Member findMember = query.selectFrom(QMember.member).fetchOne();

        Member firstMember = query.selectFrom(QMember.member)
                .fetchFirst();

        QueryResults<Member> results = query.selectFrom(member).fetchResults();

        results.getTotal();
        List<Member> content = results.getResults();

        long l = query.selectFrom(member)
                .fetchCount();
    }

    /*
     *
     *
     * */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> members = query.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(),
                        member.username.asc().nullsLast())
                .fetch();

        Member member5 = members.get(0);
        Member member6 = members.get(1);
        Member memberNull = members.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging() {
        QueryResults<Member> result = query.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2).fetchResults();


        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getTotal()).isEqualTo(4);
    }

    @Test
    public void aggregation() {
        List<Tuple> result = query
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                ).from(member).fetch();

        Tuple tuple = result.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(400);
        assertThat(tuple.get(member.age.avg())).isEqualTo(100);
        assertThat(tuple.get(member.age.max())).isEqualTo(100);
        assertThat(tuple.get(member.age.min())).isEqualTo(100);

    }

    @Test
    public void group() {
        List<Tuple> result = query.select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(100);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
    }

    @Test
    public void join() {
        List<Member> result = query
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result).extracting("username")
                .containsExactly("member1", "member2");
    }

    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> fetch = query
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(fetch)
                .extracting("username")
                .containsExactly("teamA","teamB");
    }

    @Test
    public void joinOnFiltering(){

        List<Tuple> teamA = query.select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : teamA) {
            System.out.println("tuple = " + tuple);
        }

    }
}
