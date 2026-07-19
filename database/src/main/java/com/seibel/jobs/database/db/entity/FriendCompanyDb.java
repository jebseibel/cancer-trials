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
@Table(name = "friend_company", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"friend_id", "company_id"})
})
public class FriendCompanyDb extends BaseDb {

    private static final long serialVersionUID = 1L;

    @Column(name = "friend_id", nullable = false)
    private Long friendId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;
}
