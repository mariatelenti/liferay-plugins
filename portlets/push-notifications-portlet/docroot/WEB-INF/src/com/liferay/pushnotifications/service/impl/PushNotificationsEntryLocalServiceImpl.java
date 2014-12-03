/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.pushnotifications.service.impl;

import aQute.bnd.annotation.ProviderType;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.ratings.model.RatingsStats;
import com.liferay.pushnotifications.model.PushNotificationsEntry;
import com.liferay.pushnotifications.service.base.PushNotificationsEntryLocalServiceBaseImpl;
import com.liferay.pushnotifications.util.PushNotificationsConstants;

import java.util.List;

/**
 * @author Bruno Farache
 */
@ProviderType
public class PushNotificationsEntryLocalServiceImpl
	extends PushNotificationsEntryLocalServiceBaseImpl {

	@Override
	public PushNotificationsEntry addPushNotificationsEntry(
			long userId, JSONObject payloadJSONObject)
		throws PortalException, SystemException {

		return addPushNotificationsEntry(userId, 0, payloadJSONObject);
	}

	@Override
	public PushNotificationsEntry addPushNotificationsEntry(
			long userId, long parentPushNotificationsEntryId,
			JSONObject payloadJSONObject)
		throws PortalException, SystemException {

		long pushNotificationsEntryId = counterLocalService.increment();

		PushNotificationsEntry pushNotificationsEntry =
			pushNotificationsEntryPersistence.create(pushNotificationsEntryId);

		pushNotificationsEntry.setUserId(userId);
		pushNotificationsEntry.setCreateTime(System.currentTimeMillis());

		pushNotificationsEntry.setParentPushNotificationsEntryId(
			parentPushNotificationsEntryId);

		pushNotificationsEntry.setPayload(payloadJSONObject.toString());

		pushNotificationsEntryPersistence.update(pushNotificationsEntry);

		updateChildrenPushNotificationsEntriesCount(
			parentPushNotificationsEntryId);

		sendPushNotification(userId, pushNotificationsEntry);

		return pushNotificationsEntry;
	}

	@Override
	public PushNotificationsEntry dislikePushNotificationsEntry(
			long userId, long pushNotificationsEntryId)
		throws PortalException, SystemException {

		return updateRatingsTotalScore(userId, pushNotificationsEntryId, 0);
	}

	@Override
	public List<PushNotificationsEntry> getPushNotificationsEntries(
			long parentPushNotificationsEntryId, long lastAccessTime, int start,
			int end)
		throws SystemException {

		return pushNotificationsEntryPersistence.findByC_P(
			lastAccessTime, parentPushNotificationsEntryId, start, end);
	}

	@Override
	public PushNotificationsEntry likePushNotificationsEntry(
			long userId, long pushNotificationsEntryId)
		throws PortalException, SystemException {

		return updateRatingsTotalScore(userId, pushNotificationsEntryId, 1);
	}

	@Override
	public void sendPushNotification(
			long fromUserId, PushNotificationsEntry pushNotificationsEntry)
		throws PortalException, SystemException {

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

		JSONObject fromUserJSONObject = JSONFactoryUtil.createJSONObject();

		User user = userLocalService.getUser(fromUserId);

		fromUserJSONObject.put(
			PushNotificationsConstants.KEY_FULL_NAME, user.getFullName());
		fromUserJSONObject.put(
			PushNotificationsConstants.KEY_PORTRAIT_ID, user.getPortraitId());
		fromUserJSONObject.put(
			PushNotificationsConstants.KEY_USER_ID, fromUserId);
		fromUserJSONObject.put(
			PushNotificationsConstants.KEY_UUID, user.getUuid());

		jsonObject.put(
			PushNotificationsConstants.KEY_FROM_USER, fromUserJSONObject);

		jsonObject.put(
			PushNotificationsConstants.KEY_PUSH_NOTIFICATIONS_ENTRY_ID,
			pushNotificationsEntry.getPushNotificationsEntryId());

		jsonObject.put(
			PushNotificationsConstants.KEY_PARENT_PUSH_NOTIFICATIONS_ENTRY_ID,
			pushNotificationsEntry.getParentPushNotificationsEntryId());

		JSONObject payloadJSONObject = JSONFactoryUtil.createJSONObject(
			pushNotificationsEntry.getPayload());

		jsonObject.put(
			PushNotificationsConstants.KEY_PAYLOAD, payloadJSONObject);

		pushNotificationsDeviceLocalService.sendPushNotification(
			fromUserId, jsonObject);
	}

	@Override
	public PushNotificationsEntry updateChildrenPushNotificationsEntriesCount(
			long parentPushNotificationsEntryId)
		throws PortalException, SystemException {

		if (parentPushNotificationsEntryId == 0) {
			return null;
		}

		PushNotificationsEntry pushNotificationsEntry =
			pushNotificationsEntryPersistence.findByPrimaryKey(
				parentPushNotificationsEntryId);

		int childrenPushNotificationsEntriesCount =
			pushNotificationsEntryPersistence.
				countByParentPushNotificationsEntryId(
					parentPushNotificationsEntryId);

		pushNotificationsEntry.setChildrenPushNotificationsEntriesCount(
			childrenPushNotificationsEntriesCount);

		pushNotificationsEntryPersistence.update(pushNotificationsEntry);

		return pushNotificationsEntry;
	}

	protected PushNotificationsEntry updateRatingsTotalScore(
			long userId, long pushNotificationsEntryId, long score)
		throws PortalException, SystemException {

		String className = PushNotificationsEntry.class.getName();

		ratingsEntryLocalService.updateEntry(
			userId, className, pushNotificationsEntryId, score,
			new ServiceContext());

		PushNotificationsEntry pushNotificationsEntry =
			pushNotificationsEntryPersistence.findByPrimaryKey(
				pushNotificationsEntryId);

		RatingsStats ratingsStats = ratingsStatsLocalService.getStats(
			className, pushNotificationsEntryId);

		pushNotificationsEntry.setRatingsTotalScore(
			(long)ratingsStats.getTotalScore());

		pushNotificationsEntryPersistence.update(pushNotificationsEntry);

		return pushNotificationsEntry;
	}

}