package com.example.scms.complaint;

import com.example.scms.domain.entity.Complaint;
import com.example.scms.domain.entity.ComplaintEvent;
import com.example.scms.domain.entity.ComplaintImage;
import com.example.scms.domain.entity.Department;
import com.example.scms.domain.entity.User;
import com.example.scms.domain.enums.ComplaintStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ComplaintMapper {

    public ComplaintDtos.ComplaintResponse toDetail(Complaint complaint) {
        return new ComplaintDtos.ComplaintResponse(
            complaint.getId(),
            referenceId(complaint),
            userSummary(complaint.getStudent()),
            complaint.getTitle(),
            complaint.getDescription(),
            complaint.getArea(),
            complaint.getComplaintDate(),
            complaint.getAiSeverity(),
            departmentSummary(complaint.getAiDepartment()),
            departmentSummary(complaint.getAssignedDepartment()),
            userSummary(complaint.getAssignedByAdmin()),
            complaint.getStatus(),
            complaint.getEmailSentAt(),
            complaint.getSlaDueAt(),
            complaint.getAckReceivedAt(),
            complaint.getStudentResolvedAt(),
            complaint.getEscalatedAt(),
            complaint.getCreatedAt(),
            complaint.getUpdatedAt(),
            isOverdue(complaint),
            complaint.getAiRawResponseJson(),
            complaint.getImages().stream().map(this::imageResponse).toList(),
            complaint.getEvents().stream().map(this::eventResponse).toList()
        );
    }

    public ComplaintDtos.ComplaintListItemResponse toListItem(Complaint complaint) {
        return new ComplaintDtos.ComplaintListItemResponse(
            complaint.getId(),
            referenceId(complaint),
            complaint.getTitle(),
            complaint.getArea(),
            complaint.getComplaintDate(),
            complaint.getAiSeverity(),
            complaint.getStatus(),
            isOverdue(complaint),
            departmentSummary(complaint.getAssignedDepartment()),
            complaint.getSlaDueAt(),
            complaint.getCreatedAt(),
            userSummary(complaint.getStudent())
        );
    }

    public boolean isOverdue(Complaint complaint) {
        return complaint.getStatus() == ComplaintStatus.EMAIL_SENT
            && complaint.getAckReceivedAt() == null
            && complaint.getSlaDueAt() != null
            && complaint.getSlaDueAt().isBefore(Instant.now());
    }

    private ComplaintDtos.ComplaintEventResponse eventResponse(ComplaintEvent event) {
        return new ComplaintDtos.ComplaintEventResponse(
            event.getId(),
            event.getEventType(),
            event.getMessage(),
            event.getCreatedBy(),
            event.getCreatedAt()
        );
    }

    private ComplaintDtos.ComplaintImageResponse imageResponse(ComplaintImage image) {
        return new ComplaintDtos.ComplaintImageResponse(image.getId(), image.getImageUrl(), image.getDeleteHash(), image.getCreatedAt());
    }

    private ComplaintDtos.DepartmentSummary departmentSummary(Department department) {
        if (department == null) return null;
        return new ComplaintDtos.DepartmentSummary(department.getId(), department.getName(), department.getAuthorityEmail());
    }

    private ComplaintDtos.UserSummary userSummary(User user) {
        if (user == null) return null;
        return new ComplaintDtos.UserSummary(user.getId(), user.getName(), user.getEmail());
    }

    private String referenceId(Complaint complaint) {
        return "CMP-" + complaint.getId();
    }
}
