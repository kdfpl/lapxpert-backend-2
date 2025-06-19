package com.lapxpert.backend.sanpham.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public abstract class GenericCrudService<T, ID> {
    protected abstract JpaRepository<T, ID> getRepository();

    public List<T> findAll() {
        return getRepository().findAll();
    }

    public T findById(ID id) {
        return getRepository().findById(id).orElse(null);
    }

    @Transactional
    public T save(T entity) {
        return getRepository().save(entity);
    }

    @Transactional
    public List<T> saveMultiple(List<T> entities) {
        return getRepository().saveAll(entities);
    }

    @Transactional
    public void deleteById(ID id) {
        getRepository().deleteById(id);
    }

    @Transactional
    public void deleteMultiple(List<ID> ids) {
        for (ID id : ids) {
            getRepository().deleteById(id);
        }
    }
}
