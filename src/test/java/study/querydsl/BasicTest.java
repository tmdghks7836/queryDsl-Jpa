package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
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

        Member member3 = new Member("member3", 200, teamB);
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
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> fetch = query
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(fetch)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    @Test
    public void joinOnFiltering() {

        List<Tuple> teamA = query.select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : teamA) {
            System.out.println("tuple = " + tuple);
        }

    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNO() {

        em.flush();
        em.clear();

        Member member1 = query.selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());

        assertThat(loaded).as("패치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() {

        em.flush();
        em.clear();

        Member member1 = query
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());

        assertThat(loaded).as("패치 조인 적용").isTrue();

    }

    @Test
    public void subQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = query.selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(100, 100, 200, 100);
    }

    @Test
    public void selectSubQuery() {

        QMember memberSub = new QMember("memberSub");
        List<Tuple> result = query.select(member.username,
                JPAExpressions
                        .select(member.age.avg())
                        .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    @Test
    public void basicCase() {

        List<String> fetch = query
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .when(100).then("백살")
                        .when(200).then("이백살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }


    @Test
    public void complexCase() {
        List<String> strings = query
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 30)).then("21~30")
                        .when(member.age.between(100, 199)).then("100~199")
                        .when(member.age.between(200, 300)).then("200~300")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String string : strings) {

            System.out.println("string = " + string);
        }
    }

    @Test
    public void constant() {
        List<Tuple> a = query.select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for (Tuple tuple : a) {

            System.out.println("tuple = " + tuple);
        }

    }

    @Test
    public void concat() {

        List<String> fetch = query.select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection() {
        List<String> strings = query.select(member.username)
                .from(member)
                .fetch();

        for (String string : strings) {
            System.out.println("string = " + string);
        }
    }

    @Test
    public void tupleProjection() {

        List<Tuple> fetch = query.select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            String s = tuple.get(member.username);
            Integer integer = tuple.get(member.age);
            System.out.println("s = " + s);
            System.out.println("integer = " + integer);
        }
    }

    @Test
    public void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) " +
                "from Member m", MemberDto.class).getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoBySetter() {
        List<MemberDto> fetch = query.select(
                Projections.bean(MemberDto.class, member.username, member.age)
        )
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByFields() {
        List<MemberDto> fetch = query.select(
                Projections.fields(MemberDto.class, member.username, member.age)
        )
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByConstructor() {
        List<MemberDto> fetch = query.select(
                Projections.constructor(MemberDto.class,
                        member.username,
                        member.age)
        )
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDtoByFields() {

        QMember memberSub = new QMember("memberSub");

        List<UserDto> fetch = query.select(
                Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age"))
        )
                .from(member)
                .fetch();

        for (UserDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    @Test
    public void findUserDtoByConstructor() {
        List<UserDto> fetch = query.select(
                Projections.constructor(UserDto.class,
                        member.username,
                        member.age)
        )
                .from(member)
                .fetch();

        for (UserDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = query
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder() {

        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> members = searchMember1(usernameParam, ageParam);

    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();

        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return query
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() {

        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> members = searchMember2(usernameParam, ageParam);
        assertThat(members.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameParam, Integer ageParam) {

        return query.selectFrom(member)
                .where(allEq(usernameParam, ageParam))
                .fetch();
    }

    private BooleanExpression ageEq(Integer ageParam) {

        return ageParam != null ? member.age.eq(ageParam) : null;
    }

    private BooleanExpression usernameEq(String usernameParam) {

        return usernameParam != null ? member.username.eq(usernameParam) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    public void bulkUpdate() {


        long count = query.update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(150))
                .execute();
        em.flush();
        em.clear();


        assertThat(count).isEqualTo(3);
    }

    @Test
    @Commit
    public void bulkAdd() {

        long count = query
                .update(member)
                .set(member.age, member.age.add(2))
                .execute();
        em.flush();
        em.clear();
    }

    @Test
    @Commit
    public void bulkDelete() {

        long count = query.delete(member)
                .where(member.age.gt(180))
                .execute();
        em.flush();
        em.clear();
    }

    @Test
    public void sqlFunction() {
        List<String> fetch = query
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                      member.username  , "member", "M"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2() {
        List<String> strings = query.select(member.username)
                .from(member)
                //.where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String string : strings) {
            System.out.println("string = " + string);
        }
    }

    @Test
    public void searchTest(){

    }

}
