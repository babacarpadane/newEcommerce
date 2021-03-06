package it.objectmethod.e.commerce.controller;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.objectmethod.e.commerce.controller.service.JWTService;
import it.objectmethod.e.commerce.entity.Articolo;
import it.objectmethod.e.commerce.entity.Cart;
import it.objectmethod.e.commerce.entity.CartDetail;
import it.objectmethod.e.commerce.entity.Utente;
import it.objectmethod.e.commerce.repository.ArticoloRepository;
import it.objectmethod.e.commerce.repository.CartRepository;
import it.objectmethod.e.commerce.repository.UtenteRepository;

@RestController
@RequestMapping("/api/cart")
public class CartController {
	@Autowired
	private ArticoloRepository artRep;
	@Autowired
	private UtenteRepository uteRep;
	@Autowired
	private CartRepository carRep;
	@Autowired
	private JWTService jwtSer;

	@GetMapping("/add")
	public ResponseEntity<Cart> aggiungiProdotto(@RequestParam("qta") Integer qta,
			@RequestParam("id_art") Integer idArticolo, @RequestHeader("authentificationToken") String token) {
		ResponseEntity<Cart> resp = null;
		Optional<Articolo> optArt = artRep.findById(idArticolo);

		if (optArt.isPresent() && qta > 0) {
			String nomeUtente = jwtSer.getUsername(token);
			Utente user = uteRep.findByNomeUtente(nomeUtente).get();
			Articolo art = optArt.get();
			int dispAggiornata = art.getDisponibilita() - qta;

			if (dispAggiornata >= 0) {
				art.setDisponibilita(dispAggiornata);
				art = artRep.save(art);

				Cart carrello = carRep.findByProprietarioCarrelloNomeUtente(nomeUtente);
				if (carrello == null) {
					carrello = new Cart();
					carrello.setProprietarioCarrello(user);
					carrello.setListaSpesa(new ArrayList<CartDetail>());
				}

				boolean detailNotFound = true;
				if (!carrello.getListaSpesa().isEmpty()) {
					for (CartDetail detailPresente : carrello.getListaSpesa()) {
						if (detailPresente.getArticolo().getIdArticolo().equals(art.getIdArticolo())) {
							detailPresente.setQuantita(detailPresente.getQuantita() + qta);
							detailNotFound = false;
							break;
						}
					}
				}

				if (detailNotFound) {
					CartDetail newDetail = new CartDetail();
					newDetail.setArticolo(art);
					newDetail.setQuantita(qta);
					carrello.getListaSpesa().add(newDetail);
				}

				carrello = carRep.save(carrello);
				resp = new ResponseEntity<Cart>(carrello, HttpStatus.OK);

			} else {
				resp = new ResponseEntity<Cart>(HttpStatus.BAD_REQUEST);
			}

		} else {
			resp = new ResponseEntity<Cart>(HttpStatus.BAD_REQUEST);
		}
		return resp;
	}

	@GetMapping("/remove")
	public ResponseEntity<Cart> rimuoviProdotto(@RequestParam("id_art") Integer idArticolo,
			@RequestHeader("authentificationToken") String token) {
		ResponseEntity<Cart> resp = null;
		Articolo art = artRep.findById(idArticolo).get();
		String nomeUtente = jwtSer.getUsername(token);
		Cart carrello = carRep.findByProprietarioCarrelloNomeUtente(nomeUtente);

		if (carrello != null && !carrello.getListaSpesa().isEmpty() && art != null) {
			for (CartDetail detail : carrello.getListaSpesa()) {
				if (detail.getArticolo().getIdArticolo().equals(art.getIdArticolo())) {
					art.setDisponibilita(art.getDisponibilita() + detail.getQuantita());
					carrello.getListaSpesa().remove(detail);
					carrello = carRep.save(carrello);
					resp = new ResponseEntity<Cart>(carrello, HttpStatus.OK);
					break;
				}
			}
		} else {
			resp = new ResponseEntity<Cart>(HttpStatus.BAD_REQUEST);
		}
		return resp;
	}
}