package com.learncicd.taskservice;

import com.learncicd.taskservice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public Task createTask(Task task) {
        log.info("Creating task for userId={} title={}", task.getUserId(), task.getTitle());
        return taskRepository.save(task);
    }

    public Task getTask(Long id) {
        log.debug("Fetching task with id={}", id);
        return taskRepository.findById(id).orElse(null);
    }

    public List<Task> getAllTasks() {
        log.debug("Fetching all tasks");
        return taskRepository.findAll();
    }

    public List<Task> getTasksByUser(Long userId) {
        log.info("Fetching tasks for userId={}", userId);
        return taskRepository.findByUserId(userId);
    }

    public Task updateTask(Long id, Task updatedTask) {

        log.info("Updating task id={} with new values", id);

        return taskRepository.findById(id).map(task -> {
            task.setTitle(updatedTask.getTitle());
            task.setDescription(updatedTask.getDescription());
            task.setStatus(updatedTask.getStatus());
            task.setUserId(updatedTask.getUserId());
            return taskRepository.save(task);
        }).orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    public void deleteTask(Long id) {
        log.warn("Deleting task id={}", id);
        Task deleteTask = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        taskRepository.delete(deleteTask);
    }
}
