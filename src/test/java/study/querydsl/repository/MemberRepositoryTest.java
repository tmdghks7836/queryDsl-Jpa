package study.querydsl.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @BeforeEach
    void contextLoads() {
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
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();

        assertThat(member).isEqualTo(findMember);

        List<Member> members = memberRepository.findAll();
        assertThat(members).containsExactly(member);

        List<Member> members2 = memberRepository.findByUsername("member1");
        assertThat(members2).containsExactly(member);
    }

    @Test
    public void searchTest() {

        MemberSearchCondition memberSearchCondition = new MemberSearchCondition();

        memberSearchCondition.setUsername(null);
//        memberSearchCondition.setTeamName("teamA");
//        memberSearchCondition.setAgeGoe(0);
//        memberSearchCondition.setAgeLoe(300);

        PageRequest pageRequest = PageRequest.of(1, 2);


        Page<MemberTeamDto> memberTeamDtos = memberRepository.searchPageSimple(memberSearchCondition, pageRequest);

        for (MemberTeamDto memberTeamDto : memberTeamDtos) {
            System.out.println("memberTeamDto = " + memberTeamDto);
        }
    }

    @Test
    public void predicate(){
        Iterable<Member> member1 = memberRepository.findAll(member.age.between(20, 1000).and(member.username.eq("member1")));

        for (Member member2 : member1) {
            System.out.println("member2 = " + member2);
        }
    }

}