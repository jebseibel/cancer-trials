package com.seibel.jobs.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "friend_skill", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"friend_id", "skill_id"})
})
public class FriendSkillDb extends BaseDb {

    private static final long serialVersionUID = 1L;

    @Column(name = "friend_id", nullable = false)
    private Long friendId;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;
}
