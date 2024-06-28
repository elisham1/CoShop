package com.elisham.coshop;

import android.content.Context;
import android.widget.Toast;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;
import java.util.List;

public class googleAPILocation {
    private final PlacesClient placesClient;
    private final Context context;

    public googleAPILocation(Context context, String apiKey) {
        Places.initialize(context, apiKey);
        placesClient = Places.createClient(context);
        this.context = context;
    }

    public interface OnPlacePredictionsListener {
        void onPlacePredictions(List<AutocompletePrediction> predictions);
    }

    public void getAutocompletePredictions(String query, OnPlacePredictionsListener listener) {
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
            listener.onPlacePredictions(response.getAutocompletePredictions());
        }).addOnFailureListener(exception -> {
            Toast.makeText(context, "Error getting autocomplete predictions: " + exception.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    public interface OnPlaceDetailsListener {
        void onPlaceDetails(Place place);
    }

    public void getPlaceDetails(String placeId, OnPlaceDetailsListener listener) {
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG))
                .build();

        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            listener.onPlaceDetails(response.getPlace());
        }).addOnFailureListener(exception -> {
            Toast.makeText(context, "Error getting place details: " + exception.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
