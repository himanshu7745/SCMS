package com.example.scms.scheduler;

import com.example.scms.ai.AiClassificationService;
import com.example.scms.complaint.ComplaintMapper;
import com.example.scms.complaint.ComplaintService;
import com.example.scms.domain.entity.Complaint;
import com.example.scms.domain.entity.User;
import com.example.scms.domain.enums.ComplaintEventType;
import com.example.scms.domain.enums.ComplaintStatus;
import com.example.scms.email.DepartmentMailService;
import com.example.scms.repository.ComplaintEventRepository;
import com.example.scms.repository.ComplaintRepository;
import com.example.scms.repository.DepartmentRepository;
import com.example.scms.repository.UserRepository;
import com.example.scms.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplaintSlaFlaggingTest {

    @Mock private ComplaintRepository complaintRepository;
    @Mock private ComplaintEventRepository complaintEventRepository;
    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private ComplaintMapper complaintMapper;
    @Mock private AiClassificationService aiClassificationService;
    @Mock private DepartmentMailService departmentMailService;
    @Mock private JwtService jwtService;

    private ComplaintService complaintService;

    @BeforeEach
    void setUp() {
        complaintService = new ComplaintService(
            complaintRepository,
            complaintEventRepository,
            userRepository,
            departmentRepository,
            complaintMapper,
            aiClassificationService,
            departmentMailService,
            jwtService
        );
    }

    @Test
    void flagOverdueComplaints_shouldAddEventOncePerComplaint() {
        Complaint complaint = new Complaint();
        complaint.setId(UUID.randomUUID());
        complaint.setStatus(ComplaintStatus.EMAIL_SENT);
        complaint.setSlaDueAt(Instant.now().minusSeconds(3600));
        complaint.setStudent(new User());

        when(complaintRepository.findByStatusAndSlaDueAtBeforeAndAckReceivedAtIsNull(eq(ComplaintStatus.EMAIL_SENT), any()))
            .thenReturn(List.of(complaint));
        when(complaintEventRepository.existsByComplaint_IdAndEventTypeAndMessageContaining(eq(complaint.getId()), eq(ComplaintEventType.COMMENT), anyString()))
            .thenReturn(false);

        int flagged = complaintService.flagOverdueComplaints();

        assertThat(flagged).isEqualTo(1);
        assertThat(complaint.getEvents()).hasSize(1);
        assertThat(complaint.getEvents().get(0).getEventType()).isEqualTo(ComplaintEventType.COMMENT);
    }
}
