package com.syos.service;

import com.syos.dto.CustomerRegisterRequestDTO;
import com.syos.model.User;

public abstract class RegistrationService<T extends User> {

	public final T register(CustomerRegisterRequestDTO req) {
		validate(req);
		checkUnique(req);
		@SuppressWarnings("unchecked")
		T user = (T) createUser(req);
		save(user);
		postRegister(user);
		return user;
	}

	protected abstract void validate(CustomerRegisterRequestDTO req);

	protected abstract void checkUnique(CustomerRegisterRequestDTO req);

	protected abstract User createUser(CustomerRegisterRequestDTO req);

	protected abstract void save(T user);

	protected void postRegister(T user) {
	}

}
