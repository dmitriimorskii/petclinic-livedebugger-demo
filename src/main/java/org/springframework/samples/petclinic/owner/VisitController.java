/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 * @author Wick Dynex
 */
@Controller
class VisitController {

	private static final Logger logger = LoggerFactory.getLogger(VisitController.class);

	private final OwnerRepository owners;

	private final EmailService emailService;

	private final ConcurrentLinkedQueue<EmailQueueEntry> emailQueue = new ConcurrentLinkedQueue<>();

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public VisitController(OwnerRepository owners, EmailService emailService) {
		this.owners = owners;
		this.emailService = emailService;
	}

	@PostConstruct
	public void startEmailProcessor() {
		scheduler.scheduleAtFixedRate(this::processEmailQueue, 1, 1, TimeUnit.SECONDS);
		logger.info("Email processor started, checking queue every 1 second");
	}

	@PreDestroy
	public void stopEmailProcessor() {
		scheduler.shutdown();
		logger.info("Email processor stopped");
	}

	private void processEmailQueue() {
		if (emailQueue.isEmpty()) {
			return;
		}

		Instant now = Instant.now();

		emailQueue.removeIf(entry -> {
			long secondsElapsed = now.getEpochSecond() - entry.getTimestamp().getEpochSecond();

			if (secondsElapsed >= 5) {
				try {
					emailService.sendEmail(entry.getOwner(), entry.getPet());
				} catch (Exception ex) {}
				return true; // Remove from queue
			}
			return false; // Keep in queue
		});
	}

	static class EmailQueueEntry {

		private final Owner owner;

		private final Pet pet;

		private final Instant timestamp;

		public EmailQueueEntry(Owner owner, Pet pet) {
			this.owner = owner;
			this.pet = pet;
			this.timestamp = Instant.now();
		}

		public Owner getOwner() {
			return owner;
		}

		public Pet getPet() {
			return pet;
		}

		public Instant getTimestamp() {
			return timestamp;
		}

	}

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	/**
	 * Called before each and every @RequestMapping annotated method. 2 goals: - Make sure
	 * we always have fresh data - Since we do not use the session scope, make sure that
	 * Pet object always has an id (Even though id is not part of the form fields)
	 * @param petId
	 * @return Pet
	 */
	@ModelAttribute("visit")
	public Visit loadPetWithVisit(@PathVariable("ownerId") int ownerId, @PathVariable("petId") int petId,
			Map<String, Object> model) {
		Optional<Owner> optionalOwner = owners.findById(ownerId);
		Owner owner = optionalOwner.orElseThrow(() -> new IllegalArgumentException(
				"Owner not found with id: " + ownerId + ". Please ensure the ID is correct "));

		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new IllegalArgumentException(
					"Pet with id " + petId + " not found for owner with id " + ownerId + ".");
		}
		model.put("pet", pet);
		model.put("owner", owner);

		Visit visit = new Visit();
		pet.addVisit(visit);
		return visit;
	}

	// Spring MVC calls method loadPetWithVisit(...) before initNewVisitForm is
	// called
	@GetMapping("/owners/{ownerId}/pets/{petId}/visits/new")
	public String initNewVisitForm() {
		return "pets/createOrUpdateVisitForm";
	}

	// Spring MVC calls method loadPetWithVisit(...) before processNewVisitForm is
	// called
	@PostMapping("/owners/{ownerId}/pets/{petId}/visits/new")
	public String processNewVisitForm(@ModelAttribute Owner owner, @PathVariable int petId, @Valid Visit visit,
			BindingResult result, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			return "pets/createOrUpdateVisitForm";
		}

		owner.addVisit(petId, visit);
		this.owners.save(owner);
		redirectAttributes.addFlashAttribute("message", "Your visit has been booked");

		// Add to Email queue for processing after 5 seconds
		Pet pet = owner.getPet(petId);
		emailQueue.offer(new EmailQueueEntry(owner, pet));

		return "redirect:/owners/{ownerId}";
	}

}
